<?php
	session_start();

	if($entrada = $enlace->query("SELECT * FROM " . $dbusuarios . " WHERE usuario='" . $_POST['user'] . "' AND permisos='1'")){
		if($entrada->num_rows === 0){
			echo "
			<p><div id='error'>Usuario no válido o no tiene permisos para administrar</div></p>";
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

?>
