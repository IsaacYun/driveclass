import sys
from PIL import Image, ImageDraw
src, out, preview, bg = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
SIZE = 432          # 108dp × 4 (xxxhdpi)
FIT = 0.46          # 보이는 72dp 원의 약 70%를 차지하도록
im = Image.open(src).convert("RGBA")
a = im.getchannel("A").point(lambda v: 255 if v > 8 else 0)
box = a.getbbox()
obj = im.crop(box)
s = FIT * SIZE / max(obj.size)
obj = obj.resize((round(obj.width * s), round(obj.height * s)), Image.LANCZOS)
canvas = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
canvas.alpha_composite(obj, ((SIZE - obj.width) // 2, (SIZE - obj.height) // 2))
canvas.save(out)
print("bbox", box, "->", obj.size)
# 미리보기: 런처처럼 가운데 72dp(=288px) 원으로 자른 모습
vis = Image.new("RGBA", (SIZE, SIZE), bg)
vis.alpha_composite(canvas)
m = Image.new("L", (SIZE, SIZE), 0)
ImageDraw.Draw(m).ellipse((72, 72, 360, 360), fill=255)
crop = Image.new("RGBA", (SIZE, SIZE), (255, 255, 255, 0)); crop.paste(vis, (0, 0), m)
crop.crop((72, 72, 360, 360)).save(preview)
