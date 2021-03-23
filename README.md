# chia-tractor

@see https://github.com/Chia-Network/chia-blockchain

_A tractor to help you dig in your plotting logs._

This is a short script written in Kotlin that digs through the plotting logs and saves the number of seconds that
various steps took, as well as some of the more important plotting parameters.

Currently, it spits out average plot times per temp disk, which is great for sharing
at [r/chia](https://reddit.com/r/chia)

Pull requests welcome!  I don't subdivide by platting stages, but should.

Start the Tractor: `kotlinc ./src/main/kotlin/*.kt -d tractor.jar && kotlin -classpath Tractor.jar TractorKt`

Example output:

```
Found 12 logs.
# All temp paths:
  Temp Dir: FAST2/chia-temp = average time 10.09h across 3 plot(s).
  Temp Dir: BIG/chia-temp = average time 11.452h across 6 plot(s).
  Temp Dir: FAST1/chia-temp = average time 10.011h across 3 plot(s).
# Most Recent temp paths:
  Temp Dir: FAST2/chia-temp = average time 10.508h across 1 plot(s).
  Temp Dir: BIG/chia-temp = average time 11.582h across 1 plot(s).
  Temp Dir: FAST1/chia-temp = average time 10.42h across 1 plot(s).
```
