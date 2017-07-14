<?php

	$saltar = 0;
	require_once 'permiso.php';

	if($saltar === 0){
		if($entrada = $enlace->query("SELECT * FROM " . $dbusuarios . " WHERE usuario='" . $_POST['usuario'] . "'")){
			if($entrada->num_rows !== 0){
				$entrada->close();
				try{
					$enlace->query("DELETE FROM " . $dbusuarios . " WHERE usuario='" . $_POST['usuario'] . "'");
					echo "<p><div id='ok'>Usuario eliminado correctamente</div></p>";
				} catch(Exception $error){
					die("Error al eliminar: " . $error->getMessage());
				}
				try{
					$enlace->query("UPDATE " . $dbreservas . " SET reserva='', fecha=0 WHERE reserva='" . $_POST['usuario'] . "'");
					echo "<p><div id='ok'>Instrumentos actualizados correctamente</div></p>";
				}catch(Exception $error){
					die("Error al actualizar: " . $error->getMessage());
				}
			}else{
				echo "<p><div id='error'>El usuario a eliminar no existe</div></p>";
			}
			$entrada->close();
		} else {
			die("Error al realizar la búsqueda");
		}
	}
?>
