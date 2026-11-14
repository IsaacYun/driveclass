import sys
from PIL import Image
# 3D 아이콘의 빈 여백을 잘라 512px 캔버스에 물체가 88%를 차지하도록 다시 놓는다.
src, out = sys.argv[1], sys.argv[2]
SIZE, FIT = 512, 0.88
im = Image.open(src).convert("RGBA")
box = im.getchannel("A").point(lambda v: 255 if v > 8 else 0).getbbox()
obj = im.crop(box)
s = FIT * SIZE / max(obj.size)
obj = obj.resize((round(obj.width * s), round(obj.height * s)), Image.LANCZOS)
c = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
c.alpha_composite(obj, ((SIZE - obj.width) // 2, (SIZE - obj.height) // 2))
c.save(out)
