<?php
	if($entrada = $enlace->query("SELECT * FROM " . $dbusuarios . " WHERE usuario='" . $_REQUEST['admin'] . "'")){
		if($entrada->num_rows === 0){
			echo "<p><div id='error'>El usuario que autoriza no existe</div></p>";
			$saltar = 1;
		}else{
			$comprobar = hash("sha256", $_REQUEST['adcont']);
			foreach($entrada as $fila){
				if($fila['contrasena'] !== $comprobar){
					echo "<p><div id='error'>La contraseña del que autoriza no es correcta</div></p>";
					$saltar = 1;
				} elseif($fila['permisos'] === '0'){
					echo "<p><div id='error'>El usuario no tiene permisos</div></p>";
					$saltar = 1;
				}
			}
		}
		$entrada->close();
	}else{
		die("Error al realizar la búsqueda");
	}
?>
