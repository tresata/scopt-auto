![Build Status](https://github.com/tresata/scopt-auto/actions/workflows/ci.yml/badge.svg)

# scopt-auto
Scopt-auto is an attempt to automatically create a scopt parser for a case class representing the target configuration.

The main ideas are:
* Parameters in the case class with default values becomes optional command line options. Parameters without default values become mandatory command line options.
* Nested case classes should be supported. This way these nested case classes can represent frequently re-used configurations without copy/paste.

The practical reality is:
* A camelCase parameter in a case class becomes a --snake-case argument. 
* A parameter in a nested case class is represented by the dot notation (`.`) in the parser option. So for example `config.nestedConfig.nestedSetting` (config here is a value of our configuration case class) becomes the command line option `--nested-config.nested-setting`.
* Only one level of case class nesting is supported, simply because i couldn't find out how to do a second level using shapeless.
* To create a command line option that is optional without a default value use a case class parameter of type `Option[T]` with default value `None`.
* No Scala 3 support, we aren't using it yet...

A simple example:
```
scala> :paste
// Entering paste mode (ctrl-D to finish)

import com.tresata.scopt.auto.Options

case class Cpu(mhz: Int, cores: Int = 4)
case class Drive(size: Int, opal: Boolean = false)
case class Laptop(serial: Option[String] = None, cpu: Cpu, drive: Drive)

// Exiting paste mode, now interpreting.

import com.tresata.scopt.auto.Options
class Cpu
class Drive
class Laptop

scala> Options[Laptop].parse("--cpu.mhz 3000 --drive.size 2000")
val res0: Laptop = Laptop(None,Cpu(3000,4),Drive(2000,false))

scala> Options[Laptop].parse("--serial 123456 --cpu.mhz 2000 --cpu.cores 2 --drive.size 1000 --drive.opal true")
val res1: Laptop = Laptop(Some(123456),Cpu(2000,2),Drive(1000,true))

scala> Options[Laptop].parser
val res2: scopt.OParser[_, Laptop] = scopt.OParser@54812c74

scala> Options[Laptop].parse("--serial 123456 --cpu.mhz 2000 --drive.opal true")
Error: Missing option --drive.size
Try --help for more information.
java.lang.IllegalArgumentException: invalid arguments
  at com.tresata.scopt.auto.Options$$anon$1.parse(Options.scala:40)
  ...

scala> Options[Laptop].parse("--help")
Usage:  [options]

  --serial <value>      optional Option[String] (default None)
  --cpu.mhz <value>     required Int
  --cpu.cores <value>   optional Int (default 4)
  --drive.size <value>  required Int
  --drive.opal <value>  optional Boolean (default false)
  --help
```

Have fun!
Team @ Tresata
