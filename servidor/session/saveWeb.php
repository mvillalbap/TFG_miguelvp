<?php

	if($entrada = $enlace->query("SELECT * FROM " . $dbusuarios . " WHERE usuario='" . $_POST['user'] . "'")){
		if($entrada->num_rows === 0){
			echo "
			<p><div id='error'>Usuario no válido</div></p>";
			$no_query = 1;
		}else{
			$comparar = hash("sha256", $_POST['cont']);
			foreach($entrada as $fila){
				if($fila['contrasena'] === $comparar){
					$no_query = 0;
				}else{
					echo "
					<p><div id='error'>Contraseña incorrecta</div></p>";
					$no_query = 1;
				}
			}
		}
		$entrada->close();
	} else {
		die("Error al realizar la búsqueda");
	}

	$accion;

	if($no_query === 0){
		if($entrada = $enlace->query("SELECT * FROM " . $dbreservas . " WHERE nombre='" . $_POST['inst'] . "'")){
                	if($entrada->num_rows === 0){
                        	$entrada->close();
                        	print("Null\n");
                	}else{
                        	foreach($entrada as $fila){
                                	if($fila['reserva'] === $_POST['user']){
                                        	try{
                                                	$enlace->query("UPDATE " . $dbreservas . " SET reserva='', fecha=0 WHERE nombre='" . $_POST['inst'] . "'");
							$accion = 'liberar';
                                        	}catch(Exception $error){
                                                	die("Error al actualizar: " . $error->getMessage());
                                        	}
                                	}else if($fila['reserva'] === ''){
                                        	try{
                                                	$fecha = time();
                                                	$enlace->query("UPDATE " . $dbreservas . " SET reserva='" . $_POST['user'] . "', fecha=" . $fecha . " WHERE nombre='" . $_POST['inst'] . "'");
							$accion = 'reservar';
                                        	}catch(Exception $error){
                                                	die("Error al actualizar: " . $error->getMessage());
						}
                                        }
                        	}
                	}
                	$entrada->close();
        	} else {
			die("Error al realizar la búsqueda");
	        }

		if($accion === 'liberar'){
		        $caracNoAdmit1 = array("'");
		        $caracNoAdmit2 = array("\\");
		        if(!($_POST['incidencias'] === null || $_POST['incidencias'] === '')){
		                $_POST['incidencias'] = str_replace($caracNoAdmit1, "\"", $_POST['incidencias']);
		                $_POST['incidencias'] = str_replace($caracNoAdmit2, "", $_POST['incidencias']);
		        }

		        $accesoHistorial = 1;
/*		        if($entrada = $enlace->query("SELECT * FROM " . $dbpendientes . " WHERE usuario='" . $_POST['user'] . "' AND reserva='" . $_POST['inst'] .
	                                 "' ORDER BY fecha DESC")){
		                if($entrada->num_rows !== 0){
		                        foreach($entrada as $fila){
		                                try{
		                                        $enlace->query("DELETE FROM " . $dbpendientes . " WHERE id=" . $fila['id']);
		                                        $accesoHistorial = 1;
		                                }catch(Exception $error){
		                                        die("Error al borrar: " . $error->getMessage());
		                                }
		                        }
		                }
		                $entrada->close();
		        } else {
		                die("Error al realizar la búsqueda");
		        }
*/
			if($accesoHistorial === 1){

				if($_POST['_tipo'] === 'cuatro'){
		                }else if($_POST['_tipo'] === 'uno'){
		                        try{
		                                $enlace->query("INSERT INTO " . $dbhistorial . " VALUES ( null, '" . $_POST['inst'] . "', '" . $_POST['user'] . "', " . time() . ", null)");
		                        }catch(Exception $error){
		                                die("Error al actualizar: " . $error->getMessage());
		                        }
		                }else if($_POST['_tipo'] === 'dos'){
		                        if($entrada = $enlace->query("SELECT * FROM " . $dbhistorial . " WHERE reserva='" . $_POST['inst'] . "' ORDER BY fecha DESC")){
		                                if($entrada->num_rows === 0){
		                                        try{
		                                                $enlace->query("INSERT INTO " . $dbhistorial . " VALUES ( null, '" . $_POST['inst'] . "', '" . $_POST['user'] . "', " . time() . ", '" . $_POST['incidencias'] . "')");
		                                        }catch(Exception $error){
		                                                die("Error al actualizar: " . $error->getMessage());
		                                        }
		                                }else{
		                                        foreach($entrada as $fila){
		                                                try{
		                                                        $enlace->query("INSERT INTO " . $dbhistorial . " VALUES ( null, '" . $_POST['inst'] . "', '" . $_POST['user'] . "', " . time() . ", '" . $fila['incidencias'] . $_POST['incidencias'] . "\n')");
		                                                }catch(Exception $error){
		                                                        die("Error al actualizar: " . $error->getMessage());
		                                                }
		                                                break;
		                                        }
		                                }
		                        }else{
		                                die("Error en la búsqueda");
		                        }
		                        $entrada->close();
		                }else if($_POST['_tipo'] === 'tres'){
		                        try{
		                                $enlace->query("INSERT INTO " . $dbhistorial . " VALUES ( null, '" . $_POST['inst'] . "', '" . $_POST['user'] . "', " . time() . ", '" . $_POST['incidencias'] . "\n')");
		                                print("OK\n");
		                        }catch(Exception $error){
		                                die("Error al actualizar: " . $error->getMessage());
		                        }
		                }

		                $enlace->close();

			}
		}

	}

?>
