<?php
	require_once '../nombres.php';
	require_once '../conectar.php';
	require_once 'usuario.php';

	$respuesta = 0;

	if($entrada = $enlace->query("SELECT * FROM " . $dbreservas . " WHERE nombre='" . $_POST['nombre'] . "'")){
		if($entrada->num_rows === 0){
			$entrada->close();
			print("Null\n");
		}else{
			foreach($entrada as $fila){
				if($fila['reserva'] === $_POST['usuario']){
					try{
						$enlace->query("UPDATE " . $dbreservas . " SET reserva='', fecha=0 WHERE nombre='" . $_POST['nombre'] . "'");
						print("True\n");
						$respuesta = 1;
					}catch(Exception $error){
						die("Error al actualizar: " . $error->getMessage());
					}
				}else if($fila['reserva'] === ''){
					try{
						$fecha = time();
						$enlace->query("UPDATE " . $dbreservas . " SET reserva='" . $_POST['usuario'] .
 "', fecha=" . $fecha . " WHERE nombre='" . $_POST['nombre'] . "'");
						print("OK\n");
						$respuesta = 2;
					}catch(Exception $error){
						die("Error al actualizar: " . $error->getMessage());
					}
				}else{
					print("False\n");
				}
			}
		}
		$entrada->close();
	} else {
		die("Error al realizar la búsqueda");
	}

	if($respuesta === 1){
		try{
	        	$enlace->query("INSERT INTO " . $dbpendientes . " VALUES ( null, '" . $_POST['nombre'] . "', '" . $_POST['usuario'] . "', " . time() . ")");
                }catch(Exception $error){
                        die("Error al actualizar: " . $error->getMessage());
                }
	}else if($respuesta === 2){
		try{
                        $enlace->query("DELETE FROM " . $dbpendientes . " WHERE reserva='" . $_POST['nombre'] . "' AND usuario='" . $_POST['usuario'] . "' AND (fecha>=" . (time() - 60*60) . " AND fecha<=" . time() . ")");
                }catch(Exception $error){
                        die("Error al actualizar: " . $error->getMessage());
                }
	}

	$enlace->close();
?>
