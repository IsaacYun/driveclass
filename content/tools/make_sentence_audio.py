#!/usr/bin/env python3
"""
오늘의 정리 문장 오디오를 만든다(샘플 음성: macOS 음성 합성 say).
SampleCatalog.kt 의 keySentences 를 읽어 assets/sentences/<블록id>-<순번>-en.mp3, -ko.mp3 로 저장한다.
블록 오디오와 같은 목소리로 바꿀 때는 같은 파일 이름으로 덮어쓰면 된다.

    python3 content/tools/make_sentence_audio.py
"""
import pathlib, re, subprocess, tempfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
CATALOG = ROOT / "app/src/main/java/com/driveclass/app/SampleCatalog.kt"
OUT = ROOT / "app/src/main/assets/sentences"
VOICES = {"en": ("Samantha", 170), "ko": ("Yuna", 185)}  # (음성, 분당 단어 수)


def sentences():
    src = CATALOG.read_text()
    body = src[src.index("val keySentences"):src.index("val orderedIds")]
    for m in re.finditer(r'"(\w+)" to listOf\((.*?)\)\s*,?\s*(?=\n\s*"\w+" to listOf|\n\s*\))', body, re.S):
        pairs = re.findall(r'"((?:[^"\\]|\\.)*)" to "((?:[^"\\]|\\.)*)"', m.group(2))
        for i, (en, ko) in enumerate(pairs):
            yield m.group(1), i, en, ko


def render(text, voice, rate, dest):
    with tempfile.TemporaryDirectory() as tmp:
        aiff = pathlib.Path(tmp) / "a.aiff"
        subprocess.run(["say", "-v", voice, "-r", str(rate), "-o", str(aiff), text], check=True)
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", str(aiff), "-ac", "1", "-ar", "44100", "-b:a", "96k", str(dest)], check=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    n = 0
    for block, i, en, ko in sentences():
        for lang, text in (("en", en), ("ko", ko)):
            voice, rate = VOICES[lang]
            render(text, voice, rate, OUT / f"{block}-{i}-{lang}.mp3")
            n += 1
    print(f"{n} files -> {OUT}")


if __name__ == "__main__":
    main()
