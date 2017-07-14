package paquete;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;


/**
 * Created by miguelvp on 22/11/2016.
 * Clase encargada de gestionar el inicio de sesión de usuario.
 */

public class SessionActivity extends AppCompatActivity {

    private EditText intUsuario, intCont;
    private boolean ventana;
    private AlertDialog cuadro;
    private Utils utiles;
    private EditText cambio;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_session);
        intUsuario = (EditText) findViewById(R.id.Usuario);
        intCont = (EditText) findViewById(R.id.Contrasena);
        Button env = (Button) findViewById(R.id.send);
        utiles = Utils.getSingleton();
        original();

        env.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ventana = true;
                String us = intUsuario.getText().toString();
                String cont = intCont.getText().toString();
                if(sonASCII(cont)) {
                    cuadro.show();
                    Comprobar comu = new Comprobar();
                    comu.execute(us.toLowerCase(), cont);
                }else{
                    Toast.makeText(getApplicationContext(), "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
                    ventana = false;
                }
            }
        });
    }

    /**
     * Abre una ventana emergente donde se puede cambiar la dirección URI donde realizar la comunicación.
     * @param v Botón de cambio de la dirección.
     */
    public void cambiarBase(View v){
        cambio = new EditText(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.tituloActivar).setView(cambio);
        builder.setMessage(R.string.server_dir);
        builder.setPositiveButton(R.string.aceptar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                utiles.setDirBase("http://" + cambio.getText().toString());
                original();
            }
        });
        builder.setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int i) {
                original();
            }
        });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                original();
            }
        });
        cuadro = builder.create();
        cuadro.show();
    }

    /**
     * Establece la ventana emergente a la de espera.
     */
    private void original(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.server_connection);
        builder.setView(new ProgressBar(this));
        builder.setMessage(R.string.t_rec_data);
        builder.setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ventana = false;
            }
        });
        cuadro = builder.create();
    }

    /**
     * Comprueba si la cadena de caracteres pasada está compuesta por caracteres ASCII válidos.
     * @param in Cadena de caracteres a comprobar.
     * @return <ul><li>True: cadena válida.</li><li>False: cadena inválida.</li></ul>
     */
    private boolean sonASCII(String in){
        char[] cadena = in.toCharArray();
        for(char c : cadena) {
            if(((int)c)>126 || ((int)c)==39 || ((int)c)==34 || ((int)c)<32)
                return false;
        }
        return true;
    }

    /**
     * Clase encargada de gestionar la comunicación para comprobar la existencia del usuario indicado.
     */
    private class Comprobar extends AsyncTask<String, Void, InputStream> {

        private String user, contras;
        private HttpURLConnection http;

        /**
         * Inicia la comunicación con el servidor.
         * @param params Recibe como parámetros:
         *               <ul><li>[0] contiene el nombre del usuario.</li>
         *               <li>[1] contiene la contraseña del usuario sin codificar.</li></ul>
         * @return El stream recibido del servidor para su descodificación.
         */
        @Override
        protected InputStream doInBackground(String... params) {
            if (!ventana)
                return null;
            user = params[0];
            contras = params[1];
            URL url;
            URLConnection urlC;
            InputStream in = null;
            OutputStream out;
            try {
                url = new URL(utiles.getDirBase() + "/session/existe.php");
            } catch (Exception e) {
                return null;
            }
            try {
                urlC = url.openConnection();
                urlC.setReadTimeout(10000);
            } catch (IOException e){
                return null;
            }
            http = (HttpURLConnection) urlC;
            try {
                http.setReadTimeout(10000);
                http.setConnectTimeout(10000);
                http.setRequestMethod("POST");
                http.setDoOutput(true);
                http.setDoInput(true);
                out = http.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "utf-8"));
                writer.write("usuario=" + utiles.pasarHexadecimal(user) + "&cont=" + utiles.codificarRSArelleno(contras.toCharArray(), Utils.CLAVE_PUBLICA, Utils.MODULO));
                writer.flush();
                out.close();
            } catch (Exception e){
            }
            try {
                if (http.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    in = null;
                } else {
                    if (!url.getHost().equals(http.getURL().getHost())) {
                        return null;
                    }
                    in = new BufferedInputStream(http.getInputStream());
                }
            }catch (Exception e){

            }
            return in;
        }

        /**
         * Descodifica la respuesta recibida y actúa en consecuencia.
         * @param stream Stream recibido por el servidor.
         */
        @Override
        protected void onPostExecute(InputStream stream) {
            if (stream != null) {
                String datosEntrada = utiles.leerDatos(stream);
                if (datosEntrada.equals("OK\n") && ventana) {
                    Intent dev = new Intent();
                    dev.putExtra(Utils.USER_SESSION, user);
                    setResult(Utils.SESION_OK, dev);
                    http.disconnect();
                    ventana = false;
                    cuadro.dismiss();
                    finish();
                } else if (datosEntrada.equals("Sudo\n") && ventana) {
                    Intent dev = new Intent();
                    dev.putExtra(Utils.USER_SESSION, user);
                    setResult(Utils.SESION_SUDO, dev);
                    http.disconnect();
                    ventana = false;
                    cuadro.dismiss();
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.t_incorrect, Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), R.string.t_server, Toast.LENGTH_LONG).show();
            }
            http.disconnect();
            ventana = false;
            cuadro.dismiss();
        }
    }


}
