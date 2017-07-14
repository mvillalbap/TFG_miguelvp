package paquete;

import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by miguelvp on 22/11/2016.
 * Actividad de la pantalla principal.
 */

public class MainActivity extends AppCompatActivity {

    private boolean escribir;
    private String user;
    private Button reservas, conmuta;
    private TextView nombre, salir;
    private NfcAdapter adaptador;
    private AlertDialog cuadro;
    private Utils utiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Generamos la vista
        setContentView(R.layout.activity_main);

        // Obtenemos vistas para usarlas
        ImageView logo = (ImageView) findViewById(R.id.Logo);
        Button consultar = (Button) findViewById(R.id.Consultar);
        conmuta = (Button) findViewById(R.id.Conmuta);
        reservas = (Button) findViewById(R.id.Borrado);
        nombre = (TextView) findViewById(R.id.name);
        salir = (TextView) findViewById(R.id.exit);
        RelativeLayout barra = (RelativeLayout) findViewById(R.id.barra);
        RelativeLayout imagen = (RelativeLayout) findViewById(R.id.imagen);

        // Ajustamos el tamano de la imagen
        DisplayMetrics metrics = new DisplayMetrics();
        ViewGroup.LayoutParams lp = logo.getLayoutParams();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int cuadrado = metrics.widthPixels;
        int altoBotones = 3*(consultar.getLayoutParams()).height;
        int altoBarra = (barra.getLayoutParams()).height * 2;
        int hueco = metrics.heightPixels - altoBotones - altoBarra;
        lp.height = hueco;
        logo.setLayoutParams(lp);
        logo.setMaxWidth((int)(cuadrado/1.893));

        // Generamos las herramientas
        utiles = Utils.getSingleton();
        utiles.openData(this);

        // Cambio del boton de Inicio Sesion a Nuevo Tag
        conmuta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (utiles.isLogged()) {
                    nuevoTag(v);
                } else {
                    inicioSesion(v);
                }
            }
        });

        // Miramos si tiene tecnologia NFC y si esta activo
        adaptador = NfcAdapter.getDefaultAdapter(getApplicationContext());
        if (adaptador == null){
            return;
        }
        if (!adaptador.isEnabled()) {
            // Mensaje de si lo quiere activar
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.tituloActivar).setMessage(R.string.preguntaActivar);
            builder.setPositiveButton(R.string.activar, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int i) {
                    startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                }
            });
            builder.setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface d, int i){
                }
            });
            builder.setCancelable(false);
            builder.show();
        }
    }

    @Override
    protected void onStart(){
        super.onStart();
        Intent intent = getIntent();
        if (intent != null && intent.getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED)){
            if (utiles.isLogged()) {
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                Ndef nuevo = Ndef.get(tag);
                NFCadd add = new NFCadd();
                String nombre = add.leeNombre(nuevo);
                if (nombre == null) {
                    return;
                }
                // Factorizar el nombre y el numero puesto de forma aleatoria
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.sesion).setMessage(R.string.preguntaSesion);
                builder.setPositiveButton(R.string.aceptar, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int i) {
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (utiles.isSudo()){
            nombre.setText(utiles.getNombre());
            sudoButton();
        }else if (utiles.isLogged()) {
            nombre.setText(utiles.getNombre());
            logginBoton(false);
        } else {
            logginBoton(true);
        }
        escribir = false;
        utiles.setBorrado(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cuadro != null) {
            cuadro.dismiss();
        }
        desactivarEscribir();
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data){
        escribir = false;
        utiles.setBorrado(false);
        if (request == Utils.SESION){
            if (result == Utils.SESION_OK){
                user = data.getStringExtra(Utils.USER_SESSION);
                utiles.setLogged(user, this);
                nombre.setText(user.toCharArray(), 0, user.length());
                logginBoton(false);
            }else if(result == Utils.SESION_SUDO){
                user = data.getStringExtra(Utils.USER_SESSION);
                utiles.setSudo(user, this);
                nombre.setText(user.toCharArray(), 0, user.length());
                sudoButton();
            }
        } else if (request == Utils.ESCRIBIR) {
            if (result == Utils.ESCRIBIR_OK) {
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent){
        if (utiles.isLogged()) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Ndef nuevo = Ndef.get(tag);
            Intent lanz;
            NFCadd add = new NFCadd();
            if (escribir) {
                lanz = new Intent(this, NFCwrite.class);
                lanz.putExtra(Utils.TAG_EXTRA, tag);
                startActivityForResult(lanz, Utils.ESCRIBIR);
            } else {
                String nombre = add.leeNombre(nuevo);
                if (nombre == null) {
                    return;
                }
            }
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.sesion).setMessage(R.string.preguntaSesion);
            builder.setPositiveButton(R.string.aceptar, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int i) {
                }
            });
            builder.show();
        }

    }

    /**
     * Prepara el sistema para el borrado de un tag. Muestra un mensaje de advertencia y en caso
     * de aceptarlo indica a través del singleton que está en la acción de borrar.
     * @param v Botón de borrar.
     */
    public void borrarTag(View v){
        if (adaptador != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.importante).setMessage(R.string.servidor);
            builder.setPositiveButton(R.string.aceptar, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int i) {
                    cuadro.dismiss();
                    utiles.setBorrado(true);
                    cuadroTag(true);
                }
            });
            builder.setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            cuadro = builder.create();
            cuadro.show();
        }
    }

    /**
     * Lanza la actividad encargada de consultar el estado de los instrumentos.
     * @param v Botón de consultar.
     */
    public void consultar(View v){
        Intent lanz = new Intent(this, ReservasActivity.class);
        startActivity(lanz);
    }

    /**
     * Muestra en una ventana emergente los avisos creados para que puedan ser eliminados.
     * @param v Nombre de usuario.
     */
    public void consultarPeticiones(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_item);
        HashMap<String, Integer> pet = utiles.getPeticiones();
        if (pet == null)
            return;
        HashSet<String> pedidos = new HashSet<>(pet.keySet());
        for (String nombre: pedidos){
            adapter.add(nombre);
        }
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String mssg = adapter.getItem(which);
                if (mssg != null) {
                    utiles.borrarPeticion(mssg);
                }
            }
        });
        builder.show();
    }

    /**
     * Lanza la actividad encargada del inicio de sesión.
     * @param v Botón de inicio de sesión.
     */
    public void inicioSesion(View v){
        Intent lanz = new Intent(this, SessionActivity.class);
        startActivityForResult(lanz, Utils.SESION);
    }

    /**
     * Inicia el proceso de crear un nuevo tag.
     * @param v Botón de crear un nuevo tag.
     */
    public void nuevoTag(View v){
        if (adaptador != null) {
            escribir = true;
            activarEscribir();
            cuadroTag(false);
        }
    }

    /**
     * Muestra las opciones de las que dispone un usuario.
     * @param v Botón de opciones.
     */
    public void quitarSesion(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_item);
        ArrayList<String> pedidos = new ArrayList<>();
        pedidos.add(getString(R.string.sync));
        pedidos.add(getString(R.string.salir));
        for (String nombre: pedidos){
            adapter.add(nombre);
        }
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String mssg = adapter.getItem(which);
                if (mssg != null) {
                    if (mssg.equals(getString(R.string.sync))) {
                        utiles.actualizar(getApplicationContext());
                    }else if(mssg.equals(getString(R.string.salir))){
                        utiles.setLogged(null, getApplicationContext());
                        user = null;
                        logginBoton(true);
                        utiles.borrarPeticiones();
                        utiles.borrarRegistros(getApplicationContext());
                    }
                }
            }
        });
        builder.show();
    }

    /**
     * Activa el foregroundDispatch para forzar la lectura de las etiquetas que podemos codificar
     * como NDEF.
     */
    private void activarEscribir() {
        PendingIntent pendiente = PendingIntent.getActivity(getApplicationContext(),
                Utils.ESCRIBIR, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter[] filtros = new IntentFilter[1];
        filtros[0] = new IntentFilter();
        filtros[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filtros[0].addCategory(Intent.CATEGORY_DEFAULT);
            /*
            filtros[1].addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
            filtros[1].addCategory(Intent.CATEGORY_DEFAULT);
            */
        String[][] tecnologias = new String[2][1];
        tecnologias[0][0] = "android.nfc.tech.Ndef";
        tecnologias[1][0] = "android.nfc.tech.NdefFormatable";
        adaptador.enableForegroundDispatch(this, pendiente, filtros, tecnologias);
    }

    /**
     * Muestra una ventana emergente que indica las instrucciones sobre el borrado o la escritura
     * de un tag.
     * @param borrado <ul><li>True: el mensaje advierte sobre borrar.</li>
     *                <li>False: el mensaje advierte sobre escribir.</li></ul>
     */
    private void cuadroTag(boolean borrado) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (borrado)
            builder.setTitle(R.string.tituloActivar).setMessage(R.string.avisoBorrar);
        else
            builder.setTitle(R.string.tituloActivar).setMessage(R.string.aviso);
        builder.setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int i) {
                escribir = false;
                if(utiles.getBorrado())
                    utiles.setBorrado(false);
                else
                    desactivarEscribir();
            }
        });
        builder.setCancelable(false);
        cuadro = builder.create();
        cuadro.show();
    }

    /**
     * Desactiva el foregroundDispatch para no forzar la lectura del tag.
     */
    private void desactivarEscribir(){
        if(adaptador != null)
            adaptador.disableForegroundDispatch(this);
    }

    /**
     * Realiza la disposición de botones en la pantalla principal.
     * @param mostrar <ul><li>True: muestra el botón de iniciar sesión.</li>
     *                <li>False: muestra el nombre de usuario y los botones de nuevo tag y opciones.</li></ul>
     */
    private void logginBoton(boolean mostrar){
        if (mostrar)
            conmuta.setText(R.string.sesion);
        else
            conmuta.setText(R.string.tag);
        int cambio = (!mostrar)? View.VISIBLE:View.INVISIBLE;
        reservas.setVisibility(View.INVISIBLE);
        nombre.setVisibility(cambio);

        salir.setVisibility(cambio);
    }

    /**
     * Muestra la configuración de botones de la pantalla inicial para cuando el usuario tiene permisos.
     */
    private void sudoButton(){
        conmuta.setText(R.string.tag);
        reservas.setVisibility(View.VISIBLE);
        nombre.setVisibility(View.VISIBLE);
        salir.setVisibility(View.VISIBLE);
    }
}