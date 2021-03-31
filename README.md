# chia-tractor

@see https://github.com/Chia-Network/chia-blockchain

_A tractor to help you dig in your plotting logs._

This is a short script written in Kotlin that digs through the plotting logs and saves the number of seconds that
various steps took, as well as some of the more important plotting parameters.

If you have been running plotting from the command line, 
**your logs aren't saved anywhere!** (sorry about that.)
If you have been plotting from the UI, this app should find your logs in 
`$USER_HOME/.chia/mainnet/plotter/`

Currently, it spits out average plot times per temp disk, which is great for sharing
at [r/chia](https://reddit.com/r/chia)

Pull requests welcome!  I don't subdivide by plotting stages, but should.

## Build and Start the Tractor

0. Easiest: Install IntelliJ Community Edition.  If you want to go lightweight or command-line...
1. Install your tractor making tools (JRE and Kotlin)
    * Ubuntu: `sudo snap install openjdk & sudo snap install kotlin --classic & sudo apt install maven`
    * Mac: `brew install kotlin`
    * Windows: Easier to go with IntelliJ
2. Weld together the Tractor from a bucket of bolts
```
git clone https://github.com/salamanders/chia-tractor/
cd chia-tractor
mvn package
```

3. Drive around in your Tractor as often as you like!
```
export JAVA_OPTS="-Xmx8g"
kotlin -classpath ./target/consoleApp-1.0-SNAPSHOT-jar-with-dependencies.jar net.fixables.chiatractor.TractorKt
```

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
