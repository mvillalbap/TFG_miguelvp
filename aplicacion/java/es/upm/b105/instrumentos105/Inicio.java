package paquete;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by miguelvp on 01/03/2017.
 * Clase que se encarga del cargado de datos al arrancar el sistema operativo.
 */

public class Inicio extends IntentService {

    public Inicio() {
        super(".Inicio");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Utils utiles = Utils.getSingleton();
        utiles.openData(this);
        utiles.borrarPeticiones();
        if(utiles.isLogged()){
            utiles.actualizar(this);
        }
    }
}
