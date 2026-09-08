# 📏 Metrax

**Metrax** es una aplicación móvil nativa para Android diseñada para la **medición espacial en tiempo real mediante AR (Realidad Aumentada) y cámara**, calibración dinámica de escala y cálculo automático de áreas, volúmenes y materiales. 

Especialmente desarrollada para **arquitectos, ingenieros civiles, jefes de obra, constructores y profesionales de la remodelación**.

---

## 📸 Capturas de Pantalla y Demostración

| 🏠 Pantalla Principal & Calibración | 📷 Medición AR en Tiempo Real | 📊 Historial y Estimador de Insumos |
| :---: | :---: | :---: |
| <img src="docs/screenshots/home.jpg" width="260" alt="Pantalla Principal"/> | <img src="docs/screenshots/ar_measure.jpg" width="260" alt="Medición AR"/> | <img src="docs/screenshots/history.jpg" width="260" alt="Historial y Exportación"/> |
| *Modos de medición y gestor de calibración* | *Trazado de puntos en overlay sobre la cámara* | *Registro en Room, cálculo de insumos y reporte* |

---

## 🚀 Guía de Uso Paso a Paso

### 1. Calibración de Escala (`ScaleManager`)
1. Selecciona el plano de referencia (**Suelo, Pared, Mesa o Aire**).
2. Utiliza la herramienta de calibración con un objeto conocido (p. ej. tarjeta de crédito, regla estándar o longitud conocida en metros).
3. El sistema calcula automáticamente el factor de relación píxeles-a-metros ($m/px$) asegurando alta precisión geométrica.

### 2. Captura y Trazado de Puntos
1. Dirige la cámara hacia la superficie u objeto a medir.
2. Presiona sobre la pantalla (`CameraManager`) para marcar los vértices de la figura.
3. El motor visual conecta los puntos con líneas guías de alta visibilidad indicando distancias parciales en metros o pies.

### 3. Modos de Medición Espacial
* 📏 **Modo Distancia**: Mide longitudes de tramos individuales o perímetros continuos.
* 📐 **Modo Área**: Calcula el área de cualquier polígono dibujado (metros cuadrados $m^2$).
* 📦 **Modo Volumen**: Agrega la dimensión de altura para calcular el volumen total ($m^3$) de habitaciones, columnas o excavaciones.

### 4. Guardado, Estimación e Informes
* Guarda la medición en la base de datos local con título y notas de obra.
* Consulta el **Estimador de Insumos** integrado para obtener cantidades sugeridas de pintura, bolsas de cemento/hormigón o cerámicos.
* Exporta los datos registrados para compartir con el equipo o adjuntar a presupuestos.

---

## 🏗️ Versatilidad y Aplicaciones en Obra

Metrax está optimizada para acelerar las tareas rutinarias de campo en proyectos de construcción y arquitectura:

* **🧱 Cubicaciones Rápidas de Hormigón**: Calcula el volumen ($m^3$) necesario para el vertido de losas, encadenados y contrapisos directamente en la visita previa a la obra.
* **🎨 Cálculo de Insumos para Pintura y Revestimientos**: Mide áreas totales ($m^2$) de muros interiores y fachadas exteriores para proyectar la cantidad exacta de galones de pintura o m² de cerámicos/porcelanato.
* **❄️ Dimensionamiento HVAC (Aire Acondicionado)**: Obtén el volumen cúbico exacto de locales y recintos para determinar la capacidad de frigorías e instalación de climatización requerida.
* **📋 Anteproyectos y Presupuestos Exprés**: Realiza levantamientos de campo sin necesidad de cintas métricas metálicas o herramientas voluminosas.
* **⚡ Funcionamiento 100% Offline**: La aplicación almacena la información localmente en Room, garantizando su funcionamiento en subsuelos, obras alejadas o zonas sin cobertura móvil.

---

## 🛠️ Arquitectura Técnica y Tecnologías

* **Lenguaje:** Kotlin 100% Nativo
* **Interfaz:** Jetpack Compose + Material Design 3
* **Cámara y AR Canvas:** CameraX (`CameraManager`) con renderizado e interacción sobre `Canvas` de alta respuesta.
* **Motor de Escalado:** `ScaleManager` para conversión $m/px$ y transformaciones geométricas tridimensionales (`GeometryUtils`).
* **Base de Datos:** Room Database para almacenamiento local desacoplado.
* **Patrón de Diseño:** Clean Architecture + MVVM (ViewModel, MutableStateFlow, Coroutines).

---

## 📱 Compilación e Instalación

Para compilar el proyecto localmente con Android Studio:

```bash
# Clonar el repositorio
git clone https://github.com/Lucho-code/Metraje_instante.git

# Entrar al directorio
cd Metraje_instante

# Compilar la aplicación en modo Debug
./gradlew assembleDebug
```

---

*Desarrollado con Jetpack Compose y Kotlin para soluciones de ingeniería y construcción.*
