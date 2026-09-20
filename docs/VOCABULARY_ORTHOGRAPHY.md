# Vocabulario y ortografía

Definición operacional de conocimiento léxico:

```text
significado ↔ sonido ↔ escritura ↔ uso
```

Un lexema puede estar alto en reconocimiento auditivo y bajo en ortografía. Por eso esos estados se conservan separados.

## Ejercicios

- `MEANING_AB`: sonido/palabra → significado.
- `SPELLING_AB`: palabra → discriminación de escritura.
- `SPELL_FROM_AUDIO`: sonido → deletreo mental + autoevaluación.
- `TEACH` con `spellTarget`: introduce explícitamente la secuencia de letras.
- chunks: unidad multipalabra → significado/uso.

## Deletreo

`EnglishAlphabet.spellForSpeech()` produce la secuencia de letras para TTS. La capa de contenido decide cuándo deletrear: no se deletrea cada frase completa automáticamente.

## Dificultad ortográfica

`OrthographicScorer` prioriza palabras largas, familias irregulares como `-ough`/`-igh`, errores observados e importancia del lexema.
