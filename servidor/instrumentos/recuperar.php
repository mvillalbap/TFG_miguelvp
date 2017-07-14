<?php
	require_once '../nombres.php';
	require_once '../conectar.php';

	$vacio = 0;

	if($entrada = $enlace->query("SELECT * FROM " . $dbreservas . " WHERE reserva='" . $_POST['usuario'] . "'")){
		if($entrada->num_rows === 0){
			$vacio = 1;
		}else{
			foreach($entrada as $fila){
				print("nombre='" . $fila['nombre'] . "'&fecha='" . $fila['fecha'] . "'\n");
			}
		}
		$entrada->close();
	} else {
		print("Failed connection");
		die("Error al realizar la búsqueda");
	}

        if($entrada = $enlace->query("SELECT * FROM " . $dbpendientes . " WHERE usuario='" . $_POST['usuario'] . "'")){
                if($entrada->num_rows === 0){
                        if($vacio === 1)
				print("Null\n");
                }else{
                        foreach($entrada as $fila){
                                print("registro='" . $fila['reserva'] . "'&fecha='" . $fila['fecha'] . "'\n");
                        }
                }
                $entrada->close();
        } else {
                print("Failed connection");
                die("Error al realizar la búsqueda");
        }


	$enlace->close();
?>
