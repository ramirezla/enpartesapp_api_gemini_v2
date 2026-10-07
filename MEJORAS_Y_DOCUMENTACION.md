# 🚗 Documentación de Lógica y Bitácora de Mejoras - AutoPeritajeIA

**Archivo Principal:** `PresupuestoFragment.kt`  
**Propósito:** Gestiona la interfaz de usuario, la captura de datos del vehículo, fotos de evidencias y la comunicación con el modelo de IA Google Gemini para la valoración técnica de siniestros.

---

## 📌 Datos Críticos de Entrada (Valoración IA)

Estos datos afectan directamente la precisión de la estimación de costos e identificación de piezas:

1. **Marca y Modelo:** Fundamental para determinar el costo de piezas y complejidad de desmontaje (ej. *Toyota Corolla* vs. *Acura MDX*).
2. **Año del Vehículo:** Crucial para la compatibilidad de repuestos y componentes tecnológicos (sensores ADAS).
3. **Evidencias Fotográficas:** El dato primario para que el modelo de IA analice y "vea" el daño físico.
4. **Ubicación (País/Estado/Ciudad):** Ajusta los precios de repuestos y tarifas de mano de obra al mercado local.
5. **Costo de Mano de Obra por Hora:** Define la tarifa base para los cálculos matemáticos del presupuesto.
6. **Color:** Determina el costo de insumos de pintura (ej. pinturas tricapa o perladas).

---

## 🛠️ Historial y Bitácora de Mejoras Aplicadas

### 📸 1. Captura de Fotos y Gestión de Evidencias
- **Persistencia Total (`onSaveInstanceState`):** Serialización completa de `fotoList` para preservar las imágenes al pausar o girar la pantalla.
- **URIs Únicas por Posición (`uniqueName`):** Cada foto genera un identificador físico independiente (`JPEG_Peritaje_pos1_...`) evitando sobrescribir imágenes anteriores.
- **Integración con Galería (`MediaStore`):** Registro directo en la carpeta pública `Pictures/AutoPeritajeIA/`.
- **Verificación de Bytes (`hasData`):** Valida la presencia de datos en disco sin depender únicamente del resultado del Intent de la cámara.

### 💰 2. Tabla de Tarifas de Mano de Obra por País
Valores promedio de mercado en USD por hora para talleres independientes (`laborCostByCountry`):
- **Estados Unidos:** $120.0
- **Argentina:** $60.0
- **Brasil / Uruguay:** $35.0
- **Chile / Panamá:** $30.0
- **Colombia:** $28.0
- **México / Costa Rica:** $25.0
- **Ecuador / Perú / Venezuela:** $20.0
- **Bolivia / Paraguay:** $15.0 - $18.0

> **Lógica Dinámica:** `generateDamageReport()` detecta el país seleccionado en el formulario y aplica la tarifa correspondiente. Si se elige "Otra", permite la entrada manual.

### 🤖 3. Ingeniería de Prompts (Gemini IA) y Política de Reparabilidad (%)
- **Rol Pericial:** Asignación explícita del rol *"Perito Automotriz Senior"*.
- **Estructura XML y JSON:** Instrucciones delimitadas (`<datos_vehiculo>`) con esquema de salida JSON estricto.
- **Porcentaje de Prioridad de Reparación (`PORCENTAJE_REPARACION`):** Configurado en `build.gradle.kts`:
  - **Modo Debug:** `80%` (Prioridad a reparar sobre reemplazar).
  - **Modo Release:** `70%`.

| % Prioridad | Comportamiento del Modelo Gemini IA | Caso de Uso Típico |
| :---: | :--- | :--- |
| **80% - 95%** | **Máxima Reparación:** Enfocado en reparar paneles con hojalatería. Solo reemplaza si está destruido. | Aseguradoras (optimización de costos). |
| **50% - 60%** | **Equilibrado:** Evalúa si el costo de reparación supera el costo de un repuesto nuevo. | Peritaje estándar multimarca. |
| **10% - 20%** | **Preferencia por Sustitución:** Ante deformación considerable, sugiere reemplazar por pieza original. | Talleres oficiales / Concesionarios. |

---

### 📄 4. Reporte PDF Premium
- **Encabezado y Branding:** Franja superior azul con logo 3D e información general del vehículo.
- **Grilla de Fotografías:** Organización en 2 columnas con bordes elegantes.
- **Hoja Independiente de Política IA:** La tabla explicativa de la Política de Reparabilidad se imprime automáticamente en una hoja separada al final del reporte para no pisar fotos ni totales.
- **Botón Compartir:** Permite enviar el PDF generado a través de WhatsApp, correo u otras aplicaciones.

### 📍 5. Autodetección GPS y Permisos Inteligentes
- **Detección Automática:** Autocompleta País, Estado y Ciudad con el servicio `Geocoder`.
- **Adaptación API 30+ (Android 11-14):** Solicitud granular de permisos (`CAMERA`, `ACCESS_FINE_LOCATION`, `READ_MEDIA_IMAGES`) sin bucles de reinicio.
- **Ubicación GPS Obligatoria:** Verificación activa antes de permitir la navegación.

### 👤 6. Perfil del Perito y Opciones del Menú
- **Fragmento de Perfil (`ProfileFragment.kt`):** Muestra el usuario activo, rol y versión instalada (`v2.0.1`).
- **Opciones del Menú de 3 Puntos:**
  - 📸 **Guía de Fotografías:** Manual rápido con reglas de toma de evidencias.
  - 🛰️ **Actualizar Ubicación GPS:** Re-escaneo dinámico de posición.
  - 📜 **Términos y Licencia:** Marco legal y cláusulas del software.
  - ℹ️ **Acerca de:** Resumen del objetivo de AutoPeritajeIA.
