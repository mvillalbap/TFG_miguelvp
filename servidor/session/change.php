<?php

	if($_POST['cont'] !== $_POST['repet'])
		echo "<p><div id='error'>Las contraseñas no coinciden</div></p>";
	else {
		if($entrada = $enlace->query("SELECT * FROM " . $dbusuarios . " WHERE usuario='" . $_POST['usuario'] . "'")){
			if($entrada->num_rows === 0){
				echo "<p><div id='error'>El usuario no existe</div></p>";
			}else{
				$comparar = hash("sha256", $_POST['contAnt']);
				foreach($entrada as $fila){
					if($fila['contrasena'] !== $comparar){
						echo "<p><div id='error'>Contraseña incorrecta</div></p>";
					}else{
						try{
							$guardar = hash("sha256", $_POST['cont']);
							$enlace->query("UPDATE " . $dbusuarios . " SET contrasena='" . $guardar . "' WHERE usuario='" . $_POST['usuario'] . "'");
							echo "<p><div id='ok'>Se ha cambiado correctamente la contraseña</div></p>";
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
	}
?>
