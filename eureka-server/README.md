# 🧭 Eureka Server — ReservaYa

Servidor de descubrimiento y registro dinámico de servicios (**Service Registry & Discovery**) basado en **Spring Cloud Netflix Eureka**.

---

## 🎯 Responsabilidades Principales

1. **Registro Centralizado:** Permite que los microservicios (`api-gateway`, `auth-service`, `booking-service`, `payment-service`, `notification-service`) se registren dinámicamente al iniciar.
2. **Health Check y Heartbeats:** Monitorea el estado de salud de cada instancia activa mediante pulsos periódicos (heartbeats).
3. **Resolución de Nombres en el Gateway:** Provee al `api-gateway` el mapeo IP:Puerto actualizado para el balanceo de carga del lado del cliente (`lb://SERVICE-NAME`).

---

## ⚙️ Configuración (`application.yml`)

* **Puerto:** `8761`
* **Dashboard Web:** `http://localhost:8761`
* **Auto-registro:** Deshabilitado (`register-with-eureka: false`, `fetch-registry: false`) al tratarse del nodo servidor.

---

## 🖥️ Dashboard Eureka
Al levantar el servicio, puedes acceder al panel interactivo en tu navegador:
👉 **[http://localhost:8761](http://localhost:8761)**

Allí se visualizan:
* Instancias registradas actualmente en el cluster (`UP`).
* IPs y puertos asignados.
* Métricas del entorno y memoria.

---

## 🛠️ Stack Tecnológico
- **Java 17 / Spring Boot 4.1.0**
- **Spring Cloud Netflix Eureka Server**
