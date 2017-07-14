package paquete;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by miguelvp on 01/03/2017.
 * Clase que recibe y trata la señal de arranque del sistema operativo.
 */

public class Receptor extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try{
            context.startService(new Intent(context, Inicio.class));
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
