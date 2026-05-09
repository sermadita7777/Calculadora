# Calculadora Android

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Min SDK](https://img.shields.io/badge/minSdk-24-3DDC84?logo=android&logoColor=white)](https://developer.android.com/about/versions/nougat)
[![Material 3](https://img.shields.io/badge/Material-Components-757575?logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](#licencia)

Calculadora Android escrita en Kotlin, inspirada en el diseño de la app de calculadora de Xiaomi. Pensada como un proyecto pequeño pero cuidado: motor matemático con `BigDecimal`, jerarquía de operaciones, paréntesis anidados, historial persistente y modo oscuro automático.

---

## Por qué este proyecto

La calculadora del sistema operativo es uno de esos lugares donde los detalles se notan: cómo formatea los decimales, qué hace cuando pulsas dos operadores seguidos, si se cae al dividir entre cero, si recuerda lo que calculaste ayer. Quería construir una versión que no se tropezase con ninguno de esos casos y que, por encima, se sintiese bien al usarla.

El motor de cálculo es un parser recursivo descendente sobre `BigDecimal` — esto evita los errores clásicos de coma flotante (`0.1 + 0.2 ≠ 0.3` en `Double`) y soporta jerarquía de operaciones, paréntesis anidados, multiplicación implícita y unario `-`.

---

## Capturas

| Modo claro | Modo oscuro | Historial |
|---|---|---|
| _(añadir captura)_ | _(añadir captura)_ | _(añadir captura)_ |

---

## Funcionalidades

### Operaciones

- Suma, resta, multiplicación y división con jerarquía correcta (`× ÷` antes que `+ −`).
- Paréntesis con profundidad ilimitada y multiplicación implícita: `3(5+1)` → `3×(5+1)`.
- Unario `-` dentro de paréntesis: `(-5)`, `5×(-3)`.
- Auto-cierre de paréntesis pendientes al pulsar `=`.
- Porcentaje (`%`) sobre el último operando.
- Cambio de signo (`±`) que envuelve / desenvuelve el número de forma inteligente.

### UI / UX

- Diseño tipo Xiaomi: botones redondeados, paleta naranja para operadores, dígitos en gris claro/oscuro.
- **Modo oscuro automático** según el sistema (`values-night/`).
- Display con tres niveles de información: historial reciente, expresión actual y preview del resultado en vivo.
- Animación de confirmación al pulsar `=` (escala + traslación + fade).
- Status bar y navigation bar adaptadas al tema.

### Historial

- Persistente en `SharedPreferences` (hasta 100 operaciones).
- Pantalla dedicada con `RecyclerView`, fecha de cada cálculo y opción de borrar todo.
- Tap en una entrada → la inserta en la expresión actual.

### Robustez

- Cálculo con `BigDecimal` y `MathContext(34, HALF_EVEN)` — sin errores de redondeo.
- División entre cero gestionada (`Error: ÷0`, sin crash).
- No permite dos operadores seguidos (el segundo reemplaza al anterior).
- No permite dos puntos decimales en el mismo número.
- `=` con expresión incompleta queda bloqueado.
- Cualquier excepción aritmética se captura y muestra mensaje legible.
- Formato numérico forzado a `Locale.US` para que el parser pueda re-leer cualquier número que se muestre.

---

## Stack

- **Lenguaje:** Kotlin
- **UI:** Android Views + Material Components (no Compose, a propósito — ejercicio de XML clásico)
- **Persistencia:** `SharedPreferences` + JSON
- **Lista:** `RecyclerView`
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 36
- **JVM:** 11

Sin dependencias externas pesadas — sólo AppCompat, Material y RecyclerView.

---

## Cómo compilar

```bash
git clone https://github.com/sermadita7777/Calculadora.git
cd Calculadora
./gradlew assembleDebug
./gradlew installDebug   # con un dispositivo conectado
```

O directamente desde Android Studio (Hedgehog o superior): `File > Open` → seleccionar la carpeta → `Run 'app'`.

---

## Arquitectura

```
app/src/main/
├── AndroidManifest.xml
├── java/com/example/plantillatallerlayout/
│   ├── MainActivity.kt                  ← UI + orquestación
│   ├── calc/
│   │   ├── ExpressionEvaluator.kt       ← Tokenizer + parser recursivo descendente
│   │   └── NumberFormatter.kt           ← Formateo BigDecimal → String
│   └── history/
│       ├── HistoryActivity.kt           ← Pantalla de historial
│       ├── HistoryAdapter.kt            ← Adapter del RecyclerView
│       └── HistoryManager.kt            ← Persistencia en SharedPreferences (JSON)
└── res/
    ├── layout/
    │   ├── activity_main.xml            ← Calculadora principal
    │   ├── activity_history.xml         ← Pantalla de historial
    │   └── item_history.xml             ← Fila individual del historial
    ├── values/
    │   ├── colors.xml                   ← Paleta clara estilo Xiaomi
    │   ├── strings.xml
    │   └── themes.xml                   ← Theme.Calculadora + estilos de botón
    ├── values-night/
    │   ├── colors.xml                   ← Paleta oscura
    │   └── themes.xml
    └── drawable/
        ├── ic_backspace.xml
        ├── ic_history.xml
        ├── ic_back.xml
        └── ic_delete.xml
```

### Gramática del evaluador

```
expresion := termino (('+'|'-') termino)*
termino   := factor (('×'|'÷') factor)*
factor    := numero | '(' expresion ')' | '-' factor | '+' factor
```

### Flujo de cálculo

```
Pulsación de botón
        ↓
MainActivity construye/valida el string `expresion`
        ↓
ExpressionEvaluator.evaluar(expresion)
        ↓ tokenizar()  → List<Token>
        ↓ Parser       → BigDecimal
        ↓
Result (Ok / DivisionPorCero / Sintaxis / Vacia)
        ↓
NumberFormatter.formatear() → String
        ↓
Render en TextView (preview o resultado final)
```

---

## Casos de prueba manuales

| Entrada | Resultado |
|---|---|
| `2+3×4` | `14` |
| `(2+3)×4` | `20` |
| `10÷(2+3)` | `2` |
| `5÷0` | `Error: ÷0` |
| `0.1+0.2` | `0.3` (no `0.30000000000000004`) |
| `3(5)` | `15` (multiplicación implícita) |
| `5×(-3)` | `-15` |
| `((2+3)×(4-1))÷5` | `3` |
| `1.5%` | `0.015` |
| `5+5=` luego `+10=` | `20` (encadenado) |

---

## Roadmap

- [ ] Capturas de pantalla en el README.
- [ ] Calculadora científica (sin, cos, log, raíz, potencias) en modo landscape.
- [ ] Conversor de unidades.
- [ ] Tests unitarios para `ExpressionEvaluator` y `NumberFormatter`.
- [ ] Migración del paquete `plantillatallerlayout` → `calculadora`.

---

## Licencia

MIT — ver el código fuente como referencia, fork y modifica libremente.
