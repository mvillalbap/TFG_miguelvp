<?php
        if($_SERVER['REQUEST_METHOD'] != 'POST'){
                die("No se puede acceder a la página por un método distinto a POST");
        }

        $enlace = new mysqli($dbhost, $dbuser, $dbpsswd, $dbname);

        if($enlace->connect_errno){
                die("Failed connection");
        }
?>
