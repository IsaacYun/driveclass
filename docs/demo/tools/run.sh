#!/bin/bash
# 시연 녹화: 탭 시각을 녹화 시작 기준으로 taps.log에 남긴다.
A=~/Library/Android/sdk/platform-tools/adb
L=$(dirname "$0")/taps.log; : > $L
$A shell rm -f /sdcard/demo2.mp4
$A shell screenrecord --size 1920x1080 --bit-rate 16000000 --time-limit 90 /sdcard/demo2.mp4 &
REC=$!
sleep 0.6
T0=$(perl -MTime::HiRes=time -e 'printf "%.3f", time')
at() { local t=$1; shift; local d=$1; shift
  perl -MTime::HiRes=time,sleep -e "my \$w=$T0+$t-time; sleep(\$w) if \$w>0"
  local s=$(perl -MTime::HiRes=time -e "printf '%.2f', time-$T0")
  "$@"; echo "$t $s $d" >> $L; }
at 6.5  play        $A shell input tap 1693 685
at 13.8 demo1       $A shell input tap 2094 153
at 15.2 driving_on  $A shell input tap 2041 376
at 16.6 jam         $A shell input tap 1427 750
at 28.0 demo2       $A shell input tap 2094 153
at 29.6 destination $A shell input tap 1960 750
at 40.0 demo3       $A shell input tap 2094 153
at 41.6 arrive      $A shell input tap 1427 940
at 50.8 sent_play   $A shell input tap 2410 694
at 57.0 open_recap  $A shell input tap 2306 1200
at 62.3 sent_play2  $A shell input tap 2412 796
at 70.0 end true
$A shell pkill -INT screenrecord; wait $REC; sleep 1.5
$A pull /sdcard/demo2.mp4 $(dirname "$0")/raw.mp4
