// ==========================================================================
// English text for the bundled catalog. The musical structure (measures,
// chords, skill, level) lives in exploreContent.ts (Spanish base); here we
// only override the display text by id. See getExploreItems() in
// exploreContent.ts and catalogFor() in skillCatalog.ts.
// ==========================================================================

export interface ExerciseText {
  title: string
  description: string
  sectionName: string
  sectionNotes: string
}

export interface TheoryText {
  title: string
  description: string
  body: string
}

export const EN_EXERCISE_TEXT: Record<string, ExerciseText> = {
  'pima-am-e': {
    title: 'p-i-m-a over Am and E',
    description: 'The base arpeggio: each finger always plays its own string. Your first right-hand exercise.',
    sectionName: 'Am and E in quarter notes',
    sectionNotes: 'p on the bass, i=3rd, m=2nd, a=1st. Keep the hand still, only the fingers move.\nDon’t look at your right hand: memorize the distance.',
  },
  'pima-continuo-c-g': {
    title: 'Continuous p-i-m-a-m-i over C and G',
    description: 'A back-and-forth arpeggio in eighth notes, without cutting the sound when changing chords.',
    sectionName: 'C and G in eighth notes',
    sectionNotes: 'The pulse doesn’t stop at the chord change: prepare the next chord during the last two eighth notes.',
  },
  'pulgar-alternado': {
    title: 'Alternating thumb 6th–4th',
    description: 'The foundation of fingerstyle: the thumb keeps the pulse alternating two bass strings.',
    sectionName: 'Thumb only',
    sectionNotes: 'The thumb lands with the metronome, always equally strong.\nUse a rest stroke (leaning on the next string) for a solid bass.',
  },
  'pulgar-im': {
    title: 'Thumb 6-4-5-4 with i-m',
    description: 'The thumb walks three basses and the i-m fingers answer on the off-beats.',
    sectionName: 'Basses + off-beats',
    sectionNotes: 'First the thumb alone until it’s automatic; then add i and m on the “and” of each beat.',
  },
  'travis-c': {
    title: 'Basic Travis pattern over C',
    description: 'Alternating thumb bass with i-m filling in: the most-used fingerpicking pattern.',
    sectionName: 'Travis over C',
    sectionNotes: 'The thumb (5th and 4th) can’t miss: it’s your internal metronome.\nSoft accent on beats 1 and 3.',
  },
  'balada-am-c': {
    title: 'Ballad pattern over Am and C',
    description: 'A p-i-m-a-m-i-m-i pattern in eighth notes, the classic ballad accompaniment.',
    sectionName: 'Am → C',
    sectionNotes: 'Let all the strings ring: the pattern should sound like a continuous cushion.',
  },
  'cambios-em-am': {
    title: 'Changes Em ↔ Am',
    description: 'The first chord change: two neighboring shapes, the fingers barely move.',
    sectionName: 'One measure per chord',
    sectionNotes: 'Fingers 2 and 3 keep their shape and just jump strings.\nChange silently first; then with one strum per measure.',
  },
  'cambios-c-g-d': {
    title: 'Changes C ↔ G ↔ D',
    description: 'The most common open-chord progression. Goal: change without stopping the pulse.',
    sectionName: 'C – G – D – G',
    sectionNotes: 'Move the finger that travels farthest first.\nIf a change gets stuck, practise it in isolation 10 times.',
  },
  'ligados-basicos': {
    title: 'Slurs: hammer-on and pull-off',
    description: 'First slurs on open strings: the sound is produced by the left hand.',
    sectionName: 'h and p on 3rd and 2nd',
    sectionNotes: 'Hammer: strike the fret decisively, close to the fret wire.\nPull: pull the string slightly downward as you release.',
  },
  'cromatico-1234': {
    title: 'Chromatic 1-2-3-4',
    description: 'The universal finger-independence exercise: one finger per fret, moving up the strings.',
    sectionName: '6th and 5th string',
    sectionNotes: 'One finger per fret (1=index … 4=pinky).\nRaise the BPM only when every note sounds clean and even.',
  },
}

export const EN_THEORY_TEXT: Record<string, TheoryText> = {
  'teoria-leer-tab': {
    title: 'How to read a tab',
    description: 'What the lines, numbers and symbols in Muskop tabs mean.',
    body: `# How to read a tab

Each horizontal line is a **string**: the top one is the 1st (high E, e) and the bottom one the 6th (low E). The number tells you which **fret** to press; 0 is the open string.

## Timing

- The ruler on top marks the **beats** of the measure (1 2 3 4).
- Under each measure you’ll see the **note value**: ♩ quarter, ♪ eighth, 𝅘𝅥𝅯 sixteenth.
- The BPM says how many beats fit in a minute.

## Technique symbols

- **h** hammer-on (ascending slur)
- **p** pull-off (descending slur)
- **/** slide
- **b** bend and **~** vibrato
- **x** palm mute and **◇** harmonic

## The right hand

The colored letters next to the notes are the right-hand fingers: **p** thumb, **i** index, **m** middle, **a** ring.`,
  },
  'teoria-pima': {
    title: 'The right hand: PIMA',
    description: 'The golden rule of right-hand fingering in fingerstyle.',
    body: `# The right hand: PIMA

In classical notation each finger has a letter: **p** thumb, **i** index, **m** middle, **a** ring. The pinky isn’t used (or it rests).

## The golden rule

- **p** plays the bass strings: 6th, 5th and 4th.
- **i** plays the 3rd string.
- **m** plays the 2nd string.
- **a** plays the 1st string.

At first, always respect it: your hand will learn the distance to each string and you’ll be able to play without looking.

## How to place the hand

- Relaxed, slightly arched wrist, as if holding an orange.
- Thumb ahead of the fingers, never colliding with the index.
- The fingers pluck toward the palm; the thumb downward/outward.

## Common mistakes

- Staring at your right hand all the time: hide it, trust the rule.
- Tensing the wrist when you speed up: if you feel tension, lower the BPM.
- Letting the thumb sound like the fingers: the bass should carry more weight.`,
  },
  'teoria-figuras': {
    title: 'Note values, pulse and BPM',
    description: 'Whole, quarter and eighth notes: how time is split within a measure.',
    body: `# Note values, pulse and BPM

A 4/4 measure has **4 beats**. The BPM (beats per minute) says how many beats there are in a minute: at 60 BPM each beat lasts one second.

## The note values

- 𝅝 **Whole note**: lasts all 4 beats.
- 𝅗𝅥 **Half note**: 2 beats.
- ♩ **Quarter note**: 1 beat.
- ♪ **Eighth note**: half a beat (two per beat: “1 and 2 and…”).
- 𝅘𝅥𝅯 **Sixteenth note**: a quarter of a beat (four per beat).

## How to practise rhythm

- Count out loud: “1 and 2 and 3 and 4 and” for eighth notes.
- Use Muskop’s metronome: the accent marks beat 1.
- When you play a tab, the cursor shows you which note falls on each beat: watch it before you play.`,
  },
  'teoria-travis': {
    title: 'The Travis pattern, explained',
    description: 'What the alternating bass is and why it’s the basis of almost all fingerpicking.',
    body: `# The Travis pattern

The Travis pattern (after Merle Travis) is the basis of fingerpicking: the **thumb alternates two basses** on the beats, while i and m fill in the off-beats.

## The two layers

- **Layer 1 — the thumb**: beats 1, 2, 3, 4. It alternates two bass strings of the chord. It’s your internal metronome: it can never fail.
- **Layer 2 — the fingers**: i and m play on the “and” (off-beats), creating the characteristic rocking feel.

## How to build it

- Practise the thumb alone until it’s automatic.
- Add **i** only on the “and” of 1 and 3.
- Add **m** on the “and” of 2 and 4.
- Put it all together very slowly (50–60 BPM) before speeding up.

## Afterwards

Once the pattern works over one chord, practise changing chords without breaking the bass. Later the melody slips into the gaps: that’s full fingerstyle.`,
  },
}
