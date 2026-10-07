# 🚗 Documentación Técnica Completa, Bondades y Bitácora de Mejoras - AutoPeritajeIA

**Archivo Principal:** `PresupuestoFragment.kt`  
**Diseño de Interfaz:** `fragment_presupuesto.xml`  
**Propósito:** Módulo central de inspección vehicular que recopila la ficha técnica, captura evidencias fotográficas de daños, ejecuta geolocalización automática y procesa la estimación de costos de peritaje mediante el modelo de Inteligencia Artificial Google Gemini.

---

## 📌 1. Datos Críticos de Entrada (Valoración IA)

Estos datos afectan directamente la precisión en la identificación de partes afectadas y la estimación matemática de costos:

1. **Marca y Modelo:** Determina la disponibilidad de repuestos, costos de importación y complejidad de desmontaje (ej. *Toyota Corolla* vs. *Acura MDX*).
2. **Año del Vehículo:** Esencial para la compatibilidad de repuestos y la presencia de tecnologías avanzadas (sensores de estacionamiento, cámaras o sistemas ADAS).
3. **Evidencias Fotográficas:** Constituye el insumo primario que la IA analiza visualmente para clasificar la gravedad de las abolladuras, rayones y roturas.
4. **Ubicación Geográfica (País / Estado / Ciudad):** Ajusta los precios de repuestos y las tarifas de mano de obra al mercado local específico.
5. **Costo de Mano de Obra por Hora:** Establece la tarifa base por especialidad (Hojalatería, Pintura, Mecánica) para los cálculos matemáticos del informe.
6. **Color del Vehículo:** Influye en la estimación de insumos de pintura (ej. acabados perla, tricapa o metalizados vs. colores sólidos).

---

## ✨ 2. Bondades y Características Clave de `PresupuestoFragment`

### 🎨 A. Interfaz Material 3 y Experiencia Visual Premium (`fragment_presupuesto.xml`)
- **Banner Héroe con Branding 3D:** Encabezado con imagen de fondo automotriz (`fondo_app_reporte_dannos.jpg`), degradado y el isotipo 3D oficial (`logo_autoperitaje_efecto_3d.png`).
- **Diseño Basado en Tarjetas M3:** Organización en tarjetas (`CardView`) con esquinas redondeadas de 16dp, elevación sutil y separadores temáticos.
- **Campos desplegables en modo OutlinedBox:** Entradas `TextInputLayout` limpias con íconos descriptivos y menús de selección rápida.

### 🚗 B. Campos Dinámicos y Selección Flexible de Vehículos
- **Soporte de Entrada Manual ("Otra Marca" / "Otro Modelo"):** Si el vehículo no aparece en la lista de marcas o modelos populares, el sistema despliega automáticamente un campo `TextInputEditText` para escribirlo manualmente.
- **Selector Modal de Año (`NumberPicker`):** Reemplaza calendarios innecesarios con un selector rápido de año (desde 1950 hasta el año siguiente al actual).
- **Categorización por Tipo de Vehículo:** Automóviles, Camionetas, Camiones, Motocicletas, Buses, Maquinaria agrícola y Remolques.

### 🗺️ C. Jerarquía Geográfica Multinivel y Geocodificación Inversa
- **Casos de Uso Multipaís:** Cobertura de listas de estados y ciudades para Venezuela, Colombia, Ecuador, Chile, Argentina, México, Costa Rica, EE.UU., etc.
- **Spinners Enlazados Dinámicamente:** Seleccionar un Estado/Provincia filtra automáticamente las ciudades correspondientes (ej. *Carabobo* -> *Valencia, Guacara, Puerto Cabello*; *Pichincha* -> *Quito, Cayambe*).
- **Autodetección GPS (`Geocoder`):** Obtiene las coordenadas del dispositivo en tiempo real y autocompleta el País, Estado y Ciudad con un solo toque.

### 🔒 D. Protección de Pantalla durante el Análisis de IA
- **Bloqueo Anti-Doble Clic (`FLAG_NOT_TOUCHABLE`):** Durante la consulta asíncrona a la API de Gemini IA, se deshabilita la interacción táctil en toda la pantalla mediante `WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE`.
- **Indicador de Progreso:** Barra `LinearProgressIndicator` animada en la parte superior que informa al usuario que el proceso de peritaje está en curso.

### 📸 E. Captura Robusta de Fotografías
- **Visualización en Grilla Adaptativa:** Cada ítem de foto permite seleccionar la categoría (Frontal, Lateral, Posterior, VIN, Detalle) y muestra una miniatura inmediata al tomar o cargar la imagen.
- **Comprobación de Almacenamiento:** Compatible con almacenamiento en Galería pública (`Pictures/AutoPeritajeIA/`) y verificación directa de bytes grabados en disco (`hasData`).

---

## 🛠️ 3. Historial y Bitácora de Mejoras Aplicadas

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
