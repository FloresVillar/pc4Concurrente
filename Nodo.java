import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
public class Nodo {
    TCPNodo tcpnodo;
    //Pantalla pantallaNodo;
    int idObjNodo; 
    ArrayList<Data> datosEntrenamiento = new ArrayList<>();
    ArrayList<Data> datosTesteo = new ArrayList<>();
    public Nodo() {
        //pantallaNodo = new Pantalla();
    }

    public static void main(String[] args) {
        Nodo obj = new Nodo();
        obj.iniciar();
    }

    public void iniciar() {
        new Thread(() -> {
            tcpnodo = new TCPNodo("127.0.0.1", new TCPNodo.alRecibirMensaje() {
                public void mensajeRecibido(String mensaje) {
                    nodoEscuchador(mensaje);
                    //nodoEscuchadorPantalla(mensaje);
                }
            });
            tcpnodo.run();
        }).start();
    }
    //----------------------------------------------
    public void desempaquetarImagenesTrain(String mensaje){
        byte[] bytes = Base64.getDecoder().decode(mensaje.split(";")[1].trim());
            if(bytes==null){
                System.out.println("bytes == null");
            }
            else{
                String numeros = new String(bytes,StandardCharsets.UTF_8);
                String[] partes = numeros.split("\\|");
                for(String numero:partes){
                    String []componentes = numero.split(";");
                    int etiqueta = Integer.parseInt(componentes[0].trim());
                    int ancho = Integer.parseInt(componentes[1].trim());
                    int largo = Integer.parseInt(componentes[2].trim());
                    System.out.println("numero:"+etiqueta+" ancho:"+ancho+" largo:"+largo);
                    String [] pixeles = componentes[3].split(",");
                    double [] entrada= new double[ancho*largo];
                    for(int i=0;i<pixeles.length;i++){
                        String[] rgba = pixeles[i].split("-");
                        int alpha = Integer.parseInt(rgba[0].trim()); 
                        int rojo = Integer.parseInt(rgba[1].trim());
                        int verde = Integer.parseInt(rgba[2].trim());
                        int azul = Integer.parseInt(rgba[3].trim());
                        entrada[i] = (0.299*rojo+0.587*verde+0.114*azul)/255.0;
                    }
                    double [] salida =new double[10];
                    for(int i = 0;i<salida.length;i++){
                        if(i==etiqueta){
                            salida[etiqueta] = 1.0;
                        }
                    }
                    Data data = new Data(entrada,salida);
                    datosEntrenamiento.add(data);
                    }
                }
    }
    //------------------------------------------
    public void desempaquetarImagenesTesteo(String mensaje){                     // {base64}
        datosTesteo.clear();                    //"ENTRENAR;{numero;ancho;alto;mensaje|numero;...|numero;ancho;alto;mensaje|... };idObjetoCliente"
        byte[] bytes = Base64.getDecoder().decode(mensaje.split(";")[1].trim());
            if(bytes==null){
                System.out.println("bytes == null");
            }
            else{
                String numeros = new String(bytes,StandardCharsets.UTF_8);
                String[] partes = numeros.split("\\|");
                for(String numero:partes){
                    String []componentes = numero.split(";");
                    int etiqueta = Integer.parseInt(componentes[0].trim());
                    int ancho = Integer.parseInt(componentes[1].trim());
                    int largo = Integer.parseInt(componentes[2].trim());
                    System.out.println("numero:"+etiqueta+" ancho:"+ancho+" largo:"+largo);
                    String [] pixeles = componentes[3].split(",");
                    double [] entrada= new double[ancho*largo];
                    for(int i=0;i<pixeles.length;i++){
                        String[] rgba = pixeles[i].split("-");
                        int alpha = Integer.parseInt(rgba[0].trim()); 
                        int rojo = Integer.parseInt(rgba[1].trim());
                        int verde = Integer.parseInt(rgba[2].trim());
                        int azul = Integer.parseInt(rgba[3].trim());
                        entrada[i] = (0.299*rojo+0.587*verde+0.114*azul)/255.0;
                    }
                    double [] salida =new double[10];
                    for(int i = 0;i<salida.length;i++){
                        if(i==etiqueta){
                            salida[etiqueta] = 1.0;
                        }
                    }
                    Data data = new Data(entrada,salida); 
                    datosTesteo.add(data);
                    }
                }
    }
    //---------------------------------------------
    public void nodoEscuchador(String mensaje) { 
        //pantallaNodo.agregarMensaje(mensaje);
        if (mensaje.startsWith("ESTE ES TU ID")) {
            idObjNodo = Integer.parseInt(mensaje.split(";")[1].trim());
            System.out.println("ID: "+idObjNodo);
            return;
        }
        if (mensaje.startsWith("ENTRENAR")) {
            System.out.println("ENTRENAR: ");
            //String[] partes = mensaje.split(";|");
                    desempaquetarImagenesTrain(mensaje); 
                //redNeuronal(entrada ,salida)
            //creamos el modelo..
            RedNeuronal red = new RedNeuronal(784, 16, 10);
            red.entrenar(datosEntrenamiento, 300);
            accuracyEntrenamiento(red);
            datosEntrenamiento.clear();
            try {
                ObjectOutputStream paraEscribir = new ObjectOutputStream(new FileOutputStream("modelo_nodo"+idObjNodo+".dat"));
                paraEscribir.writeObject(red);
                paraEscribir.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            nodoEnvia("ENTRENAMIENTO_COMPLETADO;NODO_ID;" + idObjNodo);
        
        }
        if (mensaje.startsWith("TESTEAR")) {
            desempaquetarImagenesTesteo(mensaje);
            Data data = datosTesteo.get(0);
            try{
                ObjectInputStream modelo = new ObjectInputStream(new FileInputStream("modelo_nodo"+idObjNodo+".dat"));
                RedNeuronal red = (RedNeuronal)modelo.readObject();
                modelo.close();
                int predicho = red.predecir(data.entrada);
                int etiqueta = -1;
                for (int i=0; i<data.salida.length;i++){
                    if (data.salida[i] != 0){
                        etiqueta = i;
                        break;
                    }
                }
                nodoEnvia("TESTEO_COMPLETADO;NODO_ID;"+idObjNodo +";"+ predicho + ";" + etiqueta);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    
    }
    public void accuracyEntrenamiento(RedNeuronal red){
        int correctos = 0;
        for (Data d:datosEntrenamiento){
            int predicho = red.predecir(d.entrada);
            int real = -1;
                for (int i=0; i<d.salida.length;i++){
                    if (d.salida[i] != 0){
                        real = i;
                        break;
                    }
                }
            System.out.println("Prediccion: " + predicho + "  Real: " + real);
            if (predicho == real) correctos++;
        }
        double acc = (double) correctos/datosEntrenamiento.size();
        System.out.println("Accuracy de entrenamiento: "+ acc);
    }
    /*
    public void nodoEscuchadorPantalla(String mensaje) {
        pantallaNodo.agregarMensaje(mensaje);
    }
 */
    public void nodoEnvia(String mensaje) {
        tcpnodo.enviarMensaje(mensaje);
    }

    /*
    class Pantalla extends JFrame {
        JTextArea mensajes;
        JTextField entrada;

        Pantalla() {
            setTitle("NODO");
            setSize(300, 300);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            mensajes = new JTextArea();
            JScrollPane scroll = new JScrollPane(mensajes);
            add(scroll, BorderLayout.CENTER);
            JPanel botones = new JPanel(new BorderLayout());
            entrada = new JTextField();
            botones.add(entrada, BorderLayout.CENTER);
            ponerBoton(botones, "enviar", e -> {
                String mensj = entrada.getText();
                if (!mensj.isEmpty()) {
                    nodoEnvia(mensj);
                    entrada.setText("");
                }
            });
            add(botones, BorderLayout.SOUTH);
            setVisible(true);
        }

        public void agregarMensaje(String mensaje) {
            mensajes.append(mensaje + "\n");
        }

        public void ponerBoton(Container c, String nombre, ActionListener escuchador) {
            JButton boton = new JButton(nombre);
            c.add(boton, BorderLayout.EAST);
            boton.addActionListener(escuchador);
        }
    } */
    public class Data{
        double [] entrada;
        double [] salida; // one-hot
        Data(double[]in , double[]out){
            entrada = in; 
            salida = out;
        }
   }
   //----------------------------------------------------------------
    //--------------CLASE RED NEURONAL--------------------------------------------------
    static class RedNeuronal implements Serializable {
        double[][] w1, w2;
        double[] b1, b2;
        Random rnd = new Random();
        //-------------------------------------------------
        RedNeuronal(int pixeles, int neuronasOcultas, int neuronasSalida) {
            w1 = new double[neuronasOcultas][pixeles];
            b1 = new double[neuronasOcultas];
            w2 = new double[neuronasSalida][neuronasOcultas];
            b2 = new double[neuronasSalida];
            //inicializar(w1);
            for(int i=0;i<w1.length;i++){
                for(int j=0;j<w1[0].length;j++){
                    w1[i][j] = rnd.nextGaussian()*Math.sqrt(2.0/pixeles);
                }
            }
            //inicializar(w2);
            for(int i=0;i<w2.length;i++){
                for(int j=0;j<w2[0].length;j++){
                    w2[i][j]=rnd.nextGaussian()*Math.sqrt(2.0/neuronasOcultas);
                }
            }  
        }
        //---------------------------------------- 
        double[] propagacionAdelante(double[] x) { //capa oculta
            double[]z1 = new double[b1.length];
            for(int i=0;i<w1.length;i++){
                for(int j=0;j<x.length;j++){
                    z1[i]+=(w1[i][j]*x[j]);
                }
                z1[i]=Math.max(0,z1[i]+b1[i]);//relu
            } 
            double []z2 = new double[b2.length];
            for(int i=0;i<w2.length;i++){ //capa salida
                for(int j=0;j<z1.length;j++){
                    z2[i]+=(w2[i][j]*z1[j]);
                }
                z2[i]+=b2[i];
            }
            return softmax(z2); 
        }
        //----------------------------------------------------------
        double[] softmax(double[] z) {
            double max = java.util.Arrays.stream(z).max().getAsDouble();
            double suma = 0;
            double[] res = new double[z.length];
            for (int i = 0; i < z.length; i++){
                res[i] = Math.exp(z[i] - max);
                suma += res[i];
            }
            for (int i = 0; i < res.length; i++) {
                res[i] /= suma;
            }
            return res;
        }
        //----------------------------------------------------------
        int predecir(double[] x) {
            double[] probabilidades = propagacionAdelante(x); //softmax, las probabilidades
            int indx = 0;
            for(int i=1;i<probabilidades.length;i++){
                if(probabilidades[i]>probabilidades[indx]){
                    indx = i;
                }
            }
            return indx;//el indice(numero) con la prob mas alta
        }
        //------------------------------------------
        
        //-----------------------------------------------------------
        void entrenar(java.util.List<Data> datos, int epocas) {
            double lr = 0.001;
            for (int ep = 0; ep < epocas; ep++) {
                Collections.shuffle(datos,rnd);
                for (Data ds : datos) {
                    double[] x = ds.entrada; //cada x[i] es un pixel de los ancho*alto pixeles
                    double[] y = ds.salida; //vector one hot
                    double[] z1 = new double[b1.length];
                    double[] a1 = new double[b1.length];
                    for (int i = 0; i < w1.length; i++) {
                        for (int j = 0; j < x.length; j++)
                            z1[i] += w1[i][j] * x[j];
                        z1[i]+=b1[i];
                        a1[i] = Math.max(0, z1[i]);
                    }
                    double[] z2 = new double[b2.length];
                    double[] a2 = new double[b2.length];
                    double max = -Double.MAX_VALUE;
                    for (int i = 0; i < w2.length; i++) {
                        for (int j = 0; j < a1.length; j++)
                            z2[i] += w2[i][j] * a1[j];
                        z2[i] += b2[i];
                        if (z2[i] > max) {
                            max = z2[i];
                        }
                    }
                    double sum = 0;
                    for (int i = 0; i < a2.length; i++){
                        sum += (a2[i] = Math.exp(z2[i] - max));
                    }
                    for (int i = 0; i < a2.length; i++){
                        a2[i] /= sum;
                    } // BACKPROPAGATION                  // a2= softmax(z2)
                    double[] dLz2 = new double[y.length]; // L = -suma(yi * log(a2)) cross-entropy
                    for (int i = 0; i < y.length; i++){   // dL/z2 = suma dL/da2 * da2/z2
                        dLz2[i] = a2[i] - y[i];           //       = suma -yi dln(a2) * da2/z2
                    }                                     //       = suma -yi/a2 * d softmax(a2)/dz2 = a2-yi ver nota dL/dz2
                    double[][] dLw2 = new double[w2.length][w2[0].length];  // z2 = w2*a1 + b2
                    for (int i = 0; i < w2.length; i++)                    //  dL/dw2 = dL/z2*dz2/dw2
                        for (int j = 0; j < w2[0].length; j++)             //         = dL/z2*a1
                            dLw2[i][j] = dLz2[i] * a1[j];                   // dL/db2 = dL/dz2*dz2/db2
                    double[] dLb2 = Arrays.copyOf(dLz2, dLz2.length);       //        = dl/dz2 * 1
                    double[] dLz1 = new double[a1.length];                    //a1 = max(0,z1)
                    for (int i = 0; i < a1.length; i++) {                   //z1 = w1*x  + b1 
                        double sumD = 0;
                        for (int j = 0; j < dLz2.length; j++)                 // dL/da1 = dL/z2 * dz2/a1
                            sumD += dLz2[j] * w2[j][i];                       //        = dL/z2 * w2
                        dLz1[i] = z1[i] > 0 ? sumD : 0;                       // dL/z1 = dL/a1 * da1/dz1
                    }                                                         //       = dL/dz2 *w2 * {1,0}
                    double[][] dLw1 = new double[w1.length][w1[0].length];    //dL/dw1 = dL/z1 * dz1/dw1 
                    for (int i = 0; i < w1.length; i++)                      //       =  dL/z1 * x
                        for (int j = 0; j < x.length; j++)
                            dLw1[i][j] = dLz1[i] * x[j];
                    double[] dLb1 = Arrays.copyOf(dLz1, dLz1.length);         //dL/db1 = dL/z1 * dz1/db1
                    //DESCENSO DE GRADIENTE                                  //       =dL/dz2 * w2 * {1,0} * 1                        
                    for (int i = 0; i < w2.length; i++){                       
                        for (int j = 0; j < w2[0].length; j++){
                            w2[i][j] -= lr * dLw2[i][j];
                        }
                    }
                    for (int i = 0; i < b2.length; i++){
                        b2[i] -= lr * dLb2[i];
                    }
                    for (int i = 0; i < w1.length; i++){
                        for (int j = 0; j < w1[0].length; j++){
                            w1[i][j] -= lr * dLw1[i][j];
                        }
                    }
                    for (int i = 0; i < b1.length; i++){
                        b1[i] -= lr * dLb1[i];
                    }
                }
            }
        }
    }
}


/**
 * java.util.Arrys.stream(array).max().getAsDouble()
 *                  devuelve un wrapper    → convertir a double
 * 
 * softmax: convierte el vector de numeros reales (logits) en salidas crudas de 
 * la red softmax(zi) = exp(zi -max(zi)) / suma exp(zi -max(zi))
 * ej : si z2 = [2.0 , 1.0 , 0.1]
 * calcular las exp: e^2-2  e^1-2 e^0.1-2 suma
 * softmax(2) = e^2-2/suma *100% asi los demas    
 * dl/dz2
 * ai = e^zi / suma e^zi   S= suma e^zi
 * derivando : d/dzk (e^zi /S) 
 * d e^zi /d zk = delta e^zi   delta = 1 i=k , delta =0 i!=k 
 * d S/dzk = e^zk
 * d ai/d zk  = (delta * e^zi * S - e^zi * e^zk )/S^2
 * d ai/d zk  = e^zi/ S * (delta -  e^zk/ S )
 *  e^zi/ S =ai (softmax zi)
 *  e^zk/ S = ak (softmax zk)
 * dai/d zk = ai*(deltaik - ak)
 * O Jik = d ai/ d zk  = { ai(1-ai) i=k; -aiak i!=K} 
 * j = [ a1(1-a1)  -a1a2......-a1ak
 *       -a1a2      a2(1-a2)...-a2ak
 * 
 *         .....................ak(1-ak)]
 * 
 * dL /dzk = suma d L/ dai * dai / dzk = suma -yi/ai * ai(deltaik - ak)
 *          = suma -yi (deltaik -ak)
 *          = -yk(1 - ak) + suma (-yi)(-ak)
 *              i=k          i!=k
 *          suma yi = 1  → suma yi i!=k = 1 -yk
 *           = -yk(1 - ak) +-(1-yk)(-ak)
 *           = -yk +ykak +ak -akyk
 *             =ak -yk
 * 
 */