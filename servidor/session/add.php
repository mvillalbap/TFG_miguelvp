<?php

	$saltar = 0;
	if($_POST['empty'] !== 'yes')
		require_once 'permiso.php';

	$array = str_split($_POST['usuario']);
	$array2 = str_split($_POST['cont']);
	$n_car_cont = 0; $n_car_nom = 0;
	$errorCodigo = 0; $no_query = 0;

	foreach($array as $ind){
		$num = ord($ind);
		if($num < 48 || $num > 126 || $num === 92)
			$errorCodigo = 1;
		$n_car_nom = $n_car_nom + 1;
	}
	foreach($array2 as $i){
		$n_car_cont = $n_car_cont + 1;
	}
	if($errorCodigo === 1){
		echo "<p><div id='error'>Caracteres válidos para el nombre solo los de la tabla ascii entre los números y ~ sin \</div></p>";
		$no_query = 1;
	}
	if($n_car_nom < 4 || $n_car_nom > 30){
		echo "<p><div id='error'>La longitud del nombre debe ser entre 4 y 30 caracteres</div></p>";
		$no_query = 1;
	}
	if($n_car_cont < 6 || $n_car_cont > 30){
		echo "<p><div id='error'>La longitud de la contraseña debe ser entre 6 y 30 caracteres</div></p>";
		$no_query = 1;
	}
	if($_POST['cont'] !== $_POST['repet']){
		echo "<p><div id='error'>Las contraseñas no coinciden</div></p>";
		$no_query = 1;
	}
	if($no_query !== 1){
		if($saltar === 0){
		if($entrada = $enlace->query("SELECT * FROM " . $dbusuarios . " WHERE usuario='" . $_POST['usuario'] . "'")){
			if($entrada->num_rows === 0){
				$entrada->close();
				try{
					$permiso = ($_POST['permisos'] == 'Si')?1:0;
					$guardar = hash("sha256", $_POST['cont']);
					$enlace->query("INSERT INTO " . $dbusuarios . " values ('" . $_POST['usuario'] . "', '" . $guardar . "'," . $permiso . ")");
					echo "<p><div id='ok'>Usuario creado correctamente</div></p>";
				} catch(Exception $error){
					die("Error al insertar: " . $error->getMessage());
				}
			}else{
				echo "<p><div id='error'>El usuario ya existe</div></p>";
			}
			$entrada->close();
		} else {
			die("Error al realizar la búsqueda");
		}
		}
	}
?>
