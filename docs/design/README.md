# 화면 디자인

- `ui-mockup.html`: 시안 v1. Pleos 색 토큰과 Asta Sans만 맞춘 첫 시안(아이콘은 Tabler 대체품).
- `v2/`: 시안 v2. Pleos 디자인 시스템의 `tokens.css`와 컴포넌트 CSS(`pl-nav`, `pl-btn`, `pl-popup`, `pl-switch`, `pl-slider`)를 그대로 써서 만든 디자인 캔버스의 원본(`.dc.html`).
  - `Main`: 주행 중 재생 화면, `Parked`: 정차 중 오늘의 정리, `Settings`: 데모 설정 팝업
- 앱에 넣은 3D 아이콘: `app/src/main/res/drawable-nodpi/icon3d_*.png` (512px, 투명 배경)

## 디자인 시스템과 다른 점

- 시스템 컨트롤 아이콘(재생·다음·뒤로·설정)은 디자인 시스템 미리보기와 같은 대체 도형이다. 공식 아이콘 세트는 가이드의 Figma에만 있다.
- 코스 진행 막대와 도착 시간 칩은 디자인 시스템에 해당 컴포넌트가 없어 토큰만으로 그렸다.
- 디자인 시스템에 `switch_on` 색 토큰이 없어 스위치 켜짐 상태가 비어 보인다. 시안은 꺼짐 상태만 쓴다.

## 앱 아이콘

- Android 적응형 아이콘: 배경 `#E8F1FF`(`values/ic_launcher.xml`) + 전경 `drawable-nodpi/ic_launcher_foreground.png`, 설정은 `mipmap-anydpi-v26/ic_launcher.xml`.
- 전경은 졸업모자 쓴 재생 말풍선 3D 아이콘.
- 원본 이미지는 물체가 캔버스 가운데에 있지 않아서 `icons/center_icon.py`로 보이는 영역(알파) 기준으로 잘라 432px 레이어 가운데에 레이어 폭의 46% 크기로 다시 놓았다. Pleos 에뮬레이터 앱 라이브러리에서 가운데 정렬을 확인했다.
