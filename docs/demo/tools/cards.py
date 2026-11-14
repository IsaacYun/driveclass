"""시연 영상 v2 카드·자막 PNG. 폰트는 앱과 같은 Asta Sans."""
from PIL import Image, ImageDraw, ImageFont
import os
HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "../../../app/src/main"))
FB = f"{REPO}/res/font/asta_sans_bold.ttf"
FR = f"{REPO}/res/font/asta_sans_regular.ttf"
W, H = 1920, 1080
INK, SUB, BG = (19, 20, 23), (90, 94, 104), (246, 247, 249)
f = ImageFont.truetype

def wrap(d, text, fnt, width):
    lines, cur = [], ""
    for word in text.split(" "):
        t = (cur + " " + word).strip()
        if d.textlength(t, font=fnt) <= width: cur = t
        else: lines.append(cur); cur = word
    lines.append(cur); return lines

def caption(name, step, text, sound=False):
    """왼쪽 클러스터 영역 위 자막. sound=True면 '앱 소리' 표시(파란 점)."""
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    fb, fs = f(FB, 36), f(FB, 24)
    x0, w = 24, 592
    lines = wrap(d, text, fb, w - 72)
    h = 36 + 34 + 16 + len(lines) * 54 + 30
    y0 = 330
    d.rounded_rectangle([x0, y0, x0 + w, y0 + h], radius=36,
                        fill=(0, 82, 214, 238) if sound else (19, 20, 23, 235))
    d.text((x0 + 36, y0 + 36), step, font=fs, fill=(190, 215, 255) if sound else (120, 170, 255))
    y = y0 + 36 + 34 + 16
    for line in lines:
        d.text((x0 + 36, y), line, font=fb, fill=(255, 255, 255)); y += 54
    im.save(os.path.join(HERE, name))

def card(name, title, subtitle, notes):
    im = Image.new("RGB", (W, H), BG); d = ImageDraw.Draw(im)
    bg = Image.new("RGBA", (220, 220), (0, 0, 0, 0))
    ImageDraw.Draw(bg).rounded_rectangle([0, 0, 219, 219], radius=56, fill=(232, 241, 255))
    fg = Image.open(f"{REPO}/res/drawable-nodpi/ic_launcher_foreground.png").convert("RGBA").resize((320, 320))
    bg.alpha_composite(fg, (-50, -50)); im.paste(bg, (W // 2 - 110, 210), bg)
    for text, fnt, color, y in [(title, f(FB, 84), INK, 480), (subtitle, f(FR, 44), SUB, 600)]:
        d.text(((W - d.textlength(text, font=fnt)) / 2, y), text, font=fnt, fill=color)
    y = 720
    for n in notes:
        fn = f(FR, 32); d.text(((W - d.textlength(n, font=fn)) / 2, y), n, font=fn, fill=SUB); y += 52
    im.save(os.path.join(HERE, name))

card("title.png", "드라이브클래스 DriveClass", "도착 시간에 맞춰 끝나는 차 안의 수업",
     ["Pleos Connect 에뮬레이터 시연 · 팀 weareone"])
card("end.png", "드라이브클래스 DriveClass", "운전하는 시간이 그대로 공부하는 시간이 됩니다",
     ["남은 주행 시간으로 블록 편성 · 정체·목적지 변경 때 다시 편성",
      "도착하면 일시정지하고 오늘의 정리 · 원문/번역 문장 재생"])
CAPS = [
    ("c_navi", "내비 연동", "실제 차에서는 내비 경로 안내에서 도착까지 남은 시간을 받아 옵니다", False),
    ("c3", "오늘의 블록", "남은 시간이 12분이면, 도착 전에 끝나는 길이로 오늘의 블록을 짭니다", False),
    ("c_a1", "앱 소리 · 강의 오디오", "재생을 누르면 '회의 시작하기' 블록을 들려줍니다", True),
    ("c4", "정체 +8분", "도착이 8분 늦어지면 듣던 블록은 두고 늘어난 시간만큼 복습 블록을 더 넣습니다", False),
    ("c_a2", "앱 소리 · 강의 오디오", "코스가 바뀌어도 듣던 강의는 끊기지 않습니다", True),
    ("c5", "목적지 변경 → 6분", "남은 블록을 덜어 내고 마무리 블록으로 코스를 끝냅니다", False),
    ("c_a3", "앱 소리 · 강의 오디오", "듣던 블록은 끝까지 이어 듣습니다", True),
    ("c6", "목적지 도착", "강의를 멈추고 오늘 들은 블록과 모은 표현을 한 화면에 정리합니다", False),
    ("c7", "문장 다시 듣기", "표현 옆 재생 버튼을 누르면 원문과 번역을 차례로 들려줍니다", False),
    ("c_a4", "앱 소리 · 원문 → 번역", "Shall we get started? → 시작할까요?", True),
    ("c8", "오늘의 정리", "정차 중에는 모은 문장을 모두 다시 보고 들을 수 있습니다", False),
    ("c_a5", "앱 소리 · 원문 → 번역", "We're here to talk about the budget. → 예산 이야기를 하려고 모였습니다.", True),
]
for n, s, t, snd in CAPS: caption(n + ".png", s, t, snd)
