# 📋 Guía de Pruebas - AgenteRecomendador en Windows

## Paso 1: Compilar el proyecto

```bash
mvn clean package dependency:copy-dependencies
```

---

## Paso 2: Levantar JADE con GUI (Terminal A)

```bash
java --add-opens java.xml/com.sun.org.apache.xerces.internal.jaxp=ALL-UNNAMED -cp "target\sma-recomendacion-musical-1.0-SNAPSHOT.jar;target\dependency\*" jade.Boot -name Plataforma1 -gui -mtp "jade.mtp.http.MessageTransportProtocol(http://localhost:7778/acc)"
```

✓ Se abrirá la ventana de GUI de JADE automáticamente

---

## Paso 3: Levantar Recomendador (Terminal B)

```bash
java --add-opens java.xml/com.sun.org.apache.xerces.internal.jaxp=ALL-UNNAMED -cp "target\sma-recomendacion-musical-1.0-SNAPSHOT.jar;target\dependency\*" jade.Boot -container -host localhost RecomendadorA:com.sma.agentes.AgenteRecomendador
```

📍 En Terminal B verás:

```
[RecomendadorA] iniciando...
[RecomendadorA] Registrado en DF como recomendador-musical
[RecomendadorA] Especializado en Rock (5 canciones)
```

---

## Paso 4: Levantar DummyAgent (Terminal C)

```bash
java --add-opens java.xml/com.sun.org.apache.xerces.internal.jaxp=ALL-UNNAMED -cp "target\sma-recomendacion-musical-1.0-SNAPSHOT.jar;target\dependency\*" jade.Boot -container -host localhost Dummy:com.sma.agentes.DummyAgent
```

📍 En Terminal C verás algo como:

```
[DummyAgent] Dummy iniciado. Esperando 3 segundos...
[DummyAgent] Buscando recomendadores...
[DummyAgent] Encontrado: RecomendadorA
>>> Enviando CFP: Tarde alta energía y mood
    Contenido: Rock;5;5
>>> Enviando CFP: Noche baja energía
    Contenido: Lofi;1;2
>>> Enviando CFP: Contexto neutro
    Contenido: Rock;3;3
```

Y luego recibirá las respuestas PROPOSE:

```
[DummyAgent] ✓ PROPOSE recibido de RecomendadorA: Rock Pulse|100
[DummyAgent] ✓ PROPOSE recibido de RecomendadorA: Study Waves|78
[DummyAgent] ✓ PROPOSE recibido de RecomendadorA: Electric Road|82
```

📍 En Terminal B verás:

```
[RecomendadorA] CFP recibido de Dummy
[RecomendadorA] Contenido CFP: Rock;5;5
[RecomendadorA] PROPOSE enviado: Rock Pulse (100%)
```

---

## En la GUI de JADE

1. **Pestaña "Agents"**: Verás los 3 agentes (RecomendadorA, Dummy, df)
2. **Pestaña "Messages"**: Log de todos los mensajes CFP → PROPOSE
3. **Doble-click en agente**: Ves detalles y comportamientos activos

---

## ✅ Validación exitosa

- ✓ DummyAgent encuentra el recomendador en el DF
- ✓ Envía 3 CFP sin error
- ✓ Recibe 3 PROPOSE con canciones y puntajes
- ✓ Los puntajes son coherentes (cercanos a 100 si contexto coincide, menores si no)

---

## 🔍 Debugging - Casos esperados por contexto

| CFP Contexto | Recomendador | Mejor Canción    | Energía | Mood | Distancia | Puntaje |
| ------------ | ------------ | ---------------- | ------- | ---- | --------- | ------- |
| Rock;5;5     | A (Rock)     | Rock Pulse       | 5       | 4    | ~1.0      | ~100%   |
| Lofi;1;2     | A (Rock)     | Riff Central     | 4       | 2    | ~1.0      | ~100%   |
| Rock;3;3     | A (Rock)     | Multiple similar | 3-5     | 2-5  | ~1.4      | ~82%    |

---

## 🧪 Alternativa: Enviar CFP manual por GUI

Si quieres enviar CFP directamente sin DummyAgent:

1. Abre la GUI de JADE
2. **Tools → Send Message**
3. Completa:
   - **To:** RecomendadorA
   - **Performative:** CFP
   - **Content:** `Rock;5;5`
4. Click en Send

El recomendador responderá con PROPOSE visible en la pestaña "Messages"

---

## ❌ Troubleshooting

| Problema                         | Solución                                                                                         |
| -------------------------------- | ------------------------------------------------------------------------------------------------ |
| "No hay recomendadores en el DF" | Espera 2-3 segundos más a que se registre el Recomendador                                        |
| REFUSE "Catalogo vacio"          | El nombre del agente no contiene "A" ni "B"; renómbralo                                          |
| Error de compilación             | Verifica que Java 17+ esté instalado: `java -version`                                            |
| Puerto 7778 ocupado              | Cambia en el comando: `-mtp "jade.mtp.http.MessageTransportProtocol(http://localhost:9999/acc)"` |
