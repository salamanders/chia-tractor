# chia-tractors

@see https://github.com/Chia-Network/chia-blockchain

_A tractor to help you dig in your plotting logs._

This is a short script written in Kotlin 
that digs through the plotting logs and saves the number of seconds that various steps took, 
as well as some of the more important plotting parameters.

Currently it spits out average plot times per temp disk, 
which is great for sharing at reddit.com/r/chia

I welcome additions!

Start the Tractor: `kotlinc -script ./src/main/kotlin/tractor.kts`

```
Found 30 logs.
# All temp paths:
  Temp Dir: /media/me/FAST2/chia-temp = average time 10.09h across 9 plot(s).
  Temp Dir: /media/me/BIG/chia-temp = average time 11.452h across 12 plot(s).
  Temp Dir: /media/me/FAST1/chia-temp = average time 10.011h across 9 plot(s).
# Most Recent temp paths:
  Temp Dir: /media/me/FAST2/chia-temp = average time 10.508h across 1 plot(s).
  Temp Dir: /media/me/BIG/chia-temp = average time 11.582h across 1 plot(s).
  Temp Dir: /media/me/FAST1/chia-temp = average time 10.42h across 1 plot(s).
```