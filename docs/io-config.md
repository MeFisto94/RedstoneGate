## In-Outputs (Bidirectional Ports)
Bidirectional Ports (We'll call them "io" for brevity) are the most powerful feature of this Mod.  
They introduce loops which makes it possible to have multiple states, but loops also means you can have endless loops which would crash the server.  
To prevent this, we have some countermeasures in place. The default config variables should be okay, unless you think your users are rather exploiting this system and might not build so complex gates.

### Configuration values
We start with the default config and will explain each step a bit further:
```properties
    # When self-triggering IO Ports, honor the block's delay setting? Only relevant when io.self-triggering.allowed is true. Default: false. See https://github.com/MeFisto94/RedstoneGate/tree/master/docs/io-config.md for more info
    B:io.honor.delay=false

    # When io.honor.delay is set to false, the truth table is able to trigger itself in the same frame. This can lead to infinity-loops, if no steady state is reached. This property defines after how many loop iterations the calculation quits. A lower number reduces the server-load in case of failure, but limits the possible uses. Higher numbers rarely make sense. Default: 30. See https://github.com/MeFisto94/RedstoneGate/tree/master/docs/io-config.md for more info
    I:io.iteration-depth.max=30

    # When the io.iteration-depth.max is exceeded print a notification to the chat, so it can be fixed. Warning: If the circuit is pulsed at a high rate, this might spam your chat. Default: true.
    B:io.iteration-overflow.notify-chat=true

    # Allow Ports to be both I and O, so we handle the case of self-triggering. Default: true. See https://github.com/MeFisto94/RedstoneGate/tree/master/docs/io-config.md for more info
    B:io.self-triggering.allowed=true
```

The Properties are relatively detailed already but since this is such a complex topic and the order seems random:  
`io.self-triggering.allowed=true`: Whether Bidirectional Ports are enabled or not. If you set this to false, the IO ports wont trigger themselves, you can only use them when you trigger a state update by changing a surrounding block or redstone power level. You almost never want that, because it limits your possibilities without a real gain.  

`io.honor.delay=false`: Whether the "delay" setting of a block should be honored when self-triggering via io-ports. This setting can be useful if you want to build a pulse-generator/oscillator like this:  
![truthtable-pulsegen](https://github.com/MeFisto94/RedstoneGate/raw/master/docs/oscillator.png)
What you can see is that as soon as one pulse is added to left or right, the system will start to swing between left and right.
A good thing here is that the duration is perfectly controllable and you could also add further capabilities (like some stop signal which kills the pulse), however Redstone Gates require a bit amount of performance for this task. Note that delays <= 1 will still be counted like `io.honor.delay=false`, since a one tick delay does not make much sense in the delay system.  
The reasoning behind setting this variable to false by default was, that almost all other circuits want the signal to be propagated instantly internally (even if the output should be delayed then). It is still possible to set up oscillators by using regular redstone repeaters as delay. This makes it easy to play around with the delay times and even allows for different on and off pulsewidths, essentially implementing pulse width modulation (PWM)  

If `io.honor.delay` is `false`, the last two properties come into play.
If the delay is zero but the table still contains loops, it is possible that no steady state is reached (see the above oscillator example, since the gate always gets back to the starting state, an endless loop is created).
If that happens, the system is trying to calculate the next output forever, hanging up the server essentially.
To prevent this, there is a simple countermeasure: Count how many times we've calculated the next output (how many iterations have we done to come to the current state).
If this number is exceeded, keep the current state as-is and stop calculating.
That's defined by `io.iteration-depth.max=30`. Technically a lower value leads to less iterations in the case of endless loops, which reduces CPU load (but only IF one or more blocks are constantly hitting the max value).
If the members of the server try to crash/troll it, that setting might be the key to success, but also keep in mind that setting the value to `1` prohibits 3-state oscillators/anything.
It might be mathematically possible to calculate an upper limit (like with 6 IO ports, there can't be more than 6 states (?)), so if you can prove that, open up an issue so we could set down this value, but all in all in regular cases this value should not matter at all.  

`io.iteration-overflow.notify-chat=true`: If the above explained iteration depth is exceeded, a warning is print out into the server console where it won't ever be read. This property enables that the warning is also posted into the chat, so the players notice it and fix the bug (and these loops hitting the security mechanism _ARE_ a bug).
If the Gate is triggered periodically (through a pulse), this spams the chat with one message per pulse, which is probably what one doesn't want. Users could also abuse this to troll the server by setting up an oscillator as described above. In that case, disable the chat notification or ban them.
