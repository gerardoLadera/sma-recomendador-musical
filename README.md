# SMA: Sistema de Recomendación Musical Distribuido (JADE + Docker)

Este proyecto implementa un **Sistema Multiagente (SMA)** capaz de recomendar música de forma inteligente basándose en el contexto horario del usuario. La arquitectura es **híbrida y distribuida**, operando entre una computadora host (Windows) y contenedores Linux (Docker).

---

## Agentes del Sistema

* **Agente Analista (@P1):** Determina el contexto (Género, Energía, Mood) según la hora actual del sistema.
* **Agente Oyente (@P1):** El coordinador central. Solicita el contexto, realiza la licitación (CFP) a los recomendadores y elige la mejor opción.
* **Agente Historial (@P1):** Almacena el registro de las canciones seleccionadas.
* **Agentes Recomendadores A y B (@P2 - Docker):** Expertos en Rock y Lofi que compiten mediante el cálculo de la **Distancia Euclidiana** para ofrecer la mejor recomendación.

---

## 🛠️ Requisitos Previos

1. **Java JDK 17** o superior.
2. **Apache Maven** instalado.
3. **Docker Desktop** (en ejecución para la Plataforma 2).

---

## 📦 Compilación y Preparación

Antes de levantar los contenedores, es necesario compilar el proyecto y descargar las dependencias de JADE en la carpeta `target`:

```bash
mvn clean package dependency:copy-dependencies
```
## Ejecución - Plataforma 1 (Windows)

Se deben abrir tres terminales independientes en la raíz del proyecto para levantar los contenedores locales. (Ejecutar en el siguiente orden):

### 1. Main Container (Plataforma 1)
  Levanta la base del sistema y la interfaz gráfica (GUI) de JADE
```bash
java --add-opens java.xml/com.sun.org.apache.xerces.internal.jaxp=ALL-UNNAMED -cp "target\sma-recomendacion-musical-1.0-SNAPSHOT.jar;target\dependency\*" jade.Boot -name Plataforma1 -gui -mtp "jade.mtp.http.MessageTransportProtocol(http://localhost:7778/acc)"
```
### 2. Contenedor 1 (Agente Analista)
```bash
java --add-opens java.xml/com.sun.org.apache.xerces.internal.jaxp=ALL-UNNAMED -cp "target\sma-recomendacion-musical-1.0-SNAPSHOT.jar;target\dependency\*" jade.Boot -container -host localhost Analista:com.sma.agentes.AgenteAnalista
```
### 3. Contenedor 1 (Agente Oyente e Historial)
```bash
java --add-opens java.xml/com.sun.org.apache.xerces.internal.jaxp=ALL-UNNAMED -cp "target\sma-recomendacion-musical-1.0-SNAPSHOT.jar;target\dependency\*" jade.Boot -container -host localhost Oyente:com.sma.agentes.AgenteOyente;Historial:com.sma.agentes.AgenteHistorial
```
## 🐳 Ejecución - Plataforma 2 (Docker Linux)

Esta plataforma contiene los agentes recomendadores. Se debe de tener Docker Desktop abierto.
  1. Abre una terminal en la raíz del proyecto.
  2. Ejecuta el siguiente comando para levantar los servicios:
  ```bash
  docker-compose up
  ``` 


