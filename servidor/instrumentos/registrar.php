<?php
	require_once '../nombres.php';
	require_once '../conectar.php';
	require_once 'usuario.php';

        if($enlace->connect_errno){
                die("Failed connection");
        }

	$seleccion = "";
        $caracNoAdmit1 = array("'");
	$caracNoAdmit2 = array("\\");
        if(!($_POST['incidencias'] === null || $_POST['incidencias'] === '')){
                $_POST['incidencias'] = str_replace($caracNoAdmit1, "\"", $_POST['incidencias']);
		$_POST['incidencias'] = str_replace($caracNoAdmit2, "", $_POST['incidencias']);
        }

	$accesoHistorial = 0;
        if($entrada = $enlace->query("SELECT * FROM " . $dbpendientes . " WHERE usuario='" . $_POST['usuario'] . "' AND reserva='" . $_POST['reserva'] .
				 "' AND (fecha>=" . ($_POST['fecha'] - 60*60) . " AND fecha<=" . ($_POST['fecha'] + 60*60) . ") ORDER BY fecha DESC")){
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

/*	$maxReg = 10;
	if($entrada = $enlace->query("SELECT * FROM " . $dbhistorial . " WHERE reserva='" . $_POST['reserva'] . "' ORDER BY fecha ASC")){
		$numReg = $entrada->num_rows;
		if($numReg !== 0){
			foreach($entrada as $fila){
				if($numReg >= $maxReg || $fila['fecha'] < (time() - 60*60*24*30)){
					try{
						$enlace->query("DELETE FROM " . $dbhistorial . " WHERE id=" . $fila['id']);
						$numReg = $numReg - 1;
					}catch (Exception $error){
						die("Error al borrar antiguos: " . $error->getMessage());
					}
				}
			}
		}
		$entrada->close();
	}else{
		die("Error al abrir el enlace");
	}
*/
//	if($accesoHistorial === 1){
		if($_POST['tipo'] === '4'){
			print("OK\n");
		}else if($_POST['tipo'] === '1'){
			try{
				$enlace->query("INSERT INTO " . $dbhistorial . " VALUES ( null, '" . $_POST['reserva'] . "', '" . $_POST['usuario'] . "', " . time() . ", null)");
				print("OK\n");
			}catch(Exception $error){
				die("Error al actualizar: " . $error->getMessage());
			}
		}else if($_POST['tipo'] === '2'){
			if($entrada = $enlace->query("SELECT * FROM " . $dbhistorial . " WHERE reserva='" . $_POST['reserva'] . "' ORDER BY fecha DESC")){
	         	       if($entrada->num_rows === 0){
					try{
						$enlace->query("INSERT INTO " . $dbhistorial . " VALUES ( null, '" . $_POST['reserva'] . "', '" . $_POST['usuario'] . "', " . time() . ", '" . $_POST['incidencias'] . "')");
						print("OK\n");
					}catch(Exception $error){
						die("Error al actualizar: " . $error->getMessage());
					}
		                }else{
					foreach($entrada as $fila){
						try{
							$enlace->query("INSERT INTO " . $dbhistorial . " VALUES ( null, '" . $_POST['reserva'] . "', '" . $_POST['usuario'] . "', " . time() . ", '" . $fila['incidencias'] . $_POST['incidencias'] . "\n')");
							print("OK\n");
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
		}else if($_POST['tipo'] === '3'){
			try{
				$enlace->query("INSERT INTO " . $dbhistorial . " VALUES ( null, '" . $_POST['reserva'] . "', '" . $_POST['usuario'] . "', " . time() . ", '" . $_POST['incidencias'] . "\n')");
				print("OK\n");
			}catch(Exception $error){
				die("Error al actualizar: " . $error->getMessage());
			}
		}

		$enlace->close();
//	}
?>
