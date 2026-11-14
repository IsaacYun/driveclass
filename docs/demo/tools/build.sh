#!/bin/bash
# 시연 영상 v2 합성: 제목 6초 + 첫 화면 정지 5초 + 녹화 + 마무리 카드.
# 설명 음성(남성)과 앱 소리(강의·문장 오디오)는 시간이 겹치지 않게 배치하고, BGM은 말소리가 날 때 줄인다.
set -e
cd "$(dirname "$0")"
B=../../../app/src/main/assets/blocks
S=../../../app/src/main/assets/sentences
TOTAL=87.55
CAP=(c_navi:6.0:11.0 c3:11.0:17.5 c_a1:17.5:24.8 c4:24.8:33.0 c_a2:33.0:39.0 c5:39.0:45.4 c_a3:45.4:52.0 c6:52.0:57.5 c7:57.5:61.8 c_a4:61.8:68.0 c8:68.0:73.3 c_a5:73.3:81.35)
IN=(-loop 1 -t 6 -i title.png -i raw.mp4 -loop 1 -t 6.2 -i end.png)
n=3
FC="[0:v]fps=30,format=yuv420p,fade=in:0:12,fade=out:st=5.6:d=0.4[t];[1:v]fps=30,scale=1920:1080,setsar=1,format=yuv420p,tpad=start_duration=5:start_mode=clone,fade=in:0:9[r];[2:v]fps=30,format=yuv420p,fade=in:0:12[e];[t][r][e]concat=n=3:v=1:a=0[v0];"
prev=v0
for c in "${CAP[@]}"; do IFS=: read name a b <<< "$c"; IN+=(-i $name.png)
  FC="$FC[$prev][$n:v]overlay=enable='between(t,$a,$b)'[v$n];"; prev=v$n; n=$((n+1)); done
FC="$FC[$prev]fade=out:st=$(echo "$TOTAL-0.6" | bc):d=0.6[v];"
# 설명 음성
VO=(n1:700 n2:6300 n3:12000 n4:25000 n5:39200 n6:52000 n7:57600 n8:68300 n9:81900)
L=""
for x in "${VO[@]}"; do IFS=: read name ms <<< "$x"; IN+=(-i vo/$name.mp3)
  FC="$FC[$n:a]aresample=48000,aformat=channel_layouts=stereo,adelay=$ms|$ms[a$n];"; L="$L[a$n]"; n=$((n+1)); done
# 앱 소리: 강의(core1) 세 구간, 문장 두 개(원문→0.5초→번역)
APP=("$B/core1.mp3:0:7.0:17800" "$B/core1.mp3:15.2:21.3:33000" "$B/core1.mp3:27.6:33.2:45400")
for x in "${APP[@]}"; do IFS=: read file s e ms <<< "$x"; IN+=(-i $file); d=$(echo "$e-$s" | bc)
  FC="$FC[$n:a]aresample=48000,aformat=channel_layouts=stereo,atrim=$s:$e,asetpts=PTS-STARTPTS,afade=t=in:d=0.25,afade=t=out:st=$(echo "$d-0.4"|bc):d=0.4,volume=0.9,adelay=$ms|$ms[a$n];"; L="$L[a$n]"; n=$((n+1)); done
SEN=(core1-0-en:62000 core1-0-ko:63660 core1-1-en:73500 core1-1-ko:75770)
for x in "${SEN[@]}"; do IFS=: read name ms <<< "$x"; IN+=(-i $S/$name.mp3)
  FC="$FC[$n:a]aresample=48000,aformat=channel_layouts=stereo,volume=1.1,adelay=$ms|$ms[a$n];"; L="$L[a$n]"; n=$((n+1)); done
CNT=$(( ${#VO[@]} + ${#APP[@]} + ${#SEN[@]} ))
IN+=(-i bgm.mp3)
FC="$FC${L}amix=inputs=$CNT:normalize=0,apad,atrim=0:$TOTAL,asplit[voice][key];"
FC="$FC[$n:a]aresample=48000,aformat=channel_layouts=stereo,atrim=0:$TOTAL,volume=0.30,afade=t=in:d=1.5,afade=t=out:st=$(echo "$TOTAL-2.5"|bc):d=2.5[bg];"
FC="$FC[bg][key]sidechaincompress=threshold=0.015:ratio=10:attack=15:release=450[duck];[voice][duck]amix=inputs=2:normalize=0,alimiter=limit=0.95[a]"
ffmpeg -v error -y "${IN[@]}" -filter_complex "$FC" -map "[v]" -map "[a]" -t $TOTAL \
  -c:v libx264 -preset medium -crf 18 -pix_fmt yuv420p -r 30 -c:a aac -b:a 192k -ar 48000 -movflags +faststart weareone_DriveClass_demo.mp4
ffprobe -v error -show_entries format=duration,size -of csv=p=0 weareone_DriveClass_demo.mp4
