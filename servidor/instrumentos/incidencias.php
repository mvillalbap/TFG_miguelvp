<?php

	function pasarHexadecimal($in){
	        $devolver = "";
	        $longitud = strlen($in);
	        for($i = 0; $i < $longitud; $i = $i + 1){
	            $dec = ord(substr($in, $i, 1));
	            $hex1 = $dec/16;
	            $hex2 = $dec%16;
	            if($hex1>9)
	                $devolver = $devolver . "%" . chr($hex1+55);
	            else
	                $devolver = $devolver . "%" . chr($hex1+48);
	            if($hex2>9)
	                $devolver = $devolver . "" . chr($hex2+55);
	            else
	                $devolver = $devolver . "" . chr($hex2+48);
	        }
	        return $devolver;
	}

        require_once '../nombres.php';
        $enlace = new mysqli($dbhost,$dbuser,$dbpsswd,$dbname);

        if($enlace->connect_errno){
                die("Failed connection");
        }

        $caracNoAdmit = array("'", "\\");
        if(!($_GET['_query'] === null || $_GET['_query'] === '')){
                $_GET['_query'] = str_replace($caracNoAdmit, "", $_GET['_query']);
        }
	if($entrada = $enlace->query("SELECT * FROM " . $dbhistorial . " WHERE reserva='" . $_GET['_query'] . "' ORDER BY fecha DESC")){
		if($entrada->num_rows === 0){
			print("Null");
		}else{
			$numIncidencias = 1;
			foreach($entrada as $fila){
				print("<siguienteIncidencia'>\n");
				print("incidencias='" . pasarHexadecimal($fila['incidencias']) . "'\n");
				print("fecha=" . $fila['fecha'] . "\n");
				if($numIncidencias === intval($_GET['_cantidad'])){
					break;
				}else{
					$numIncidencias = $numIncidencias + 1;
				}
			}
		}
		$entrada->close();
	}else{
		die("Error al realizar la búsqueda");
	}

	$enlace->close();
?>
