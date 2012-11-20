/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package telnetapp;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.net.telnet.TelnetClient;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;

/**
 *
 * @author cjacuna
 */
public class DataReader implements Runnable{

    Socket sck = null;
    boolean endThread = false;
    TimeSeries[] series;
    double[] kArr = null;
    public boolean isEndThread() {
        return endThread;
    }

    public void setEndThread(boolean endThread) {
        this.endThread = endThread;
    }
    
    public DataReader(Socket sck,TimeSeries[] series,double[] kArr) {
        this.sck = sck;
        this.series = series;
        this.kArr = kArr;
    }   

    public void run() {
        FileOutputStream fout = null;
        Date now = new Date();
        byte[] b = new byte[26];
        try
        {   
            //Pruebas con simulador
            InputStream is = sck.getInputStream();
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd.HHmmss");
            DateFormat dateFormat2 = new SimpleDateFormat("HH:mm:ss");
            String fileName = dateFormat.format(now) + ".xls";
            //Imprimir fecha de inicio
            fout = new FileOutputStream(fileName);
            fout.write((dateFormat2.format(now)+"\n").getBytes());
            BufferedInputStream bfis = new BufferedInputStream(is,4096);
            while(!endThread){
                  int iRead = 0;
                  int iTotalRead = 0;
                  do{
                      iRead = bfis.read(b,iTotalRead, 26 - iTotalRead);
                      iTotalRead += iRead;
                  }while(iTotalRead < 26);
                  bfis.read(); // Cambio de línea
                  Millisecond ms = new Millisecond(now);
                  for( int i = 2, col = 0; i < b.length ; i++,col++ ){
                      double value = (0xFF & b[i++]); 
                      value +=  (0xFF & b[i]) * 256; 
                      value *= kArr[col];
                      String strValue = String.valueOf(value);
                      String csv = strValue+( (i+1) < b.length ? "\t":"");
                      fout.write(csv.getBytes());
                      series[col].addOrUpdate(ms, value);  
                      System.out.print(csv);
                  }
                  System.out.println();
                  fout.write("\n".getBytes());
                  now = new Date(now.getTime()+100);
            }     
            //Imprimir fecha final
            fout.write((dateFormat2.format(new Date())+"\n").getBytes());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.err.println("Problemas leyendo o escribiendo los datos : " + e.getMessage());
        }
        finally{
            try {
                if( sck != null ){
                    sck.close();
                }
            } catch (Exception e) {
                System.err.println("Problemas cerrando la conexión : " + e.getMessage());
            }
            try {
                if( fout != null ){
                    fout.close();
                }
            } catch (Exception e) {
                System.err.println("Problemas cerrando el archivo de salida : " + e.getMessage());
            }
        }
    }
}
