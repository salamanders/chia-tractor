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
