# chia-tractor

@see https://github.com/Chia-Network/chia-blockchain

_A tractor to help you dig in your plotting logs._

This is a short script written in Kotlin that digs through the plotting logs and saves the number of seconds that
various steps took, as well as some of the more important plotting parameters.

Currently, it spits out average plot times per temp disk, which is great for sharing
at [r/chia](https://reddit.com/r/chia)

Pull requests welcome!  I don't subdivide by platting stages, but should.

Start the Tractor: `kotlinc ./src/main/kotlin/*.kt -d tractor.jar && kotlin -classpath tractor.jar TractorKt`

Example output:

```
Found 39 completed logs.
# All temp paths:
  Temp Dir: BIG/chia-temp = average time 12.1h across 19 plot(s).
  Temp Dir: FAST1/chia-temp = average time 10.1h across 10 plot(s).
  Temp Dir: FAST2/chia-temp = average time 10.2h across 10 plot(s).
# Most Recent temp paths:
  Temp Dir: BIG/chia-temp = average time 12.8h across 1 plot(s).
  Temp Dir: FAST1/chia-temp = average time 11.1h across 1 plot(s).
  Temp Dir: FAST2/chia-temp = average time 11.1h across 1 plot(s).
# Parallel plot rate over last 4 days
  Temp Dir: BIG/chia-temp = 4.8 plots/day
  Temp Dir: FAST1/chia-temp = 2.0 plots/day
  Temp Dir: FAST2/chia-temp = 2.0 plots/day
```
