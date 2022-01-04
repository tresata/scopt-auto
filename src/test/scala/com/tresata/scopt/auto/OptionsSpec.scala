package com.tresata.scopt.auto

import java.io.File

import scopt.OParser

import org.scalatest.funspec.AnyFunSpec

object OptionsSpec {
  case class Person(firstName: String, age: Int, isEmployee: Boolean = false, petNames: Seq[String] = Seq.empty)

  case class Cpu(mhz: Int, cores: Int = 4)
  case class Drive(size: Int, opal: Boolean = false)
  case class Laptop(cpu: Cpu, drive: Drive)

  case class Server(name: String, ip: String)
  case class VM(tag: (String, String), host: Server, uptime: Int = 0)

  case class Empty()

  case class NestedEmpty(empty: Empty)

  case class Config(
    foo: Int = -1,
    out: File = new File("."),
    xyz: Boolean = false,
    libName: String = "",
    maxCount: Int = -1,
    verbose: Boolean = false,
    debug: Boolean = false,
    mode: String = "",
    files: Seq[File] = Seq(),
    keepalive: Boolean = false,
    jars: Seq[File] = Seq(),
    kwargs: Map[String, String] = Map()
  )

  case class OtherConfig(
    path: Option[File] = None
  )

  case class Defaults(x: Int = 1, y: String = "ha", z: Boolean = false)

  case class NestedDefaults(switch: Boolean = false, defaults: Defaults)
}

class OptionsSpec extends AnyFunSpec {
  import OptionsSpec._

  describe("Options") {
    it("should create parser for Person") {
      val personOptions = implicitly[Options[Person]]

      val person1 = personOptions.parse("--first-name Mary --age 31 --is-employee true --pet-names fluffy,spot")
      assert(person1 === Person("Mary", 31, true, Seq("fluffy", "spot")))

      val person2 = personOptions.parse("--first-name Bob --age 22")
      assert(person2 === Person("Bob", 22, false, Seq.empty))

      intercept[IllegalArgumentException](personOptions.parse("--first-name Bob"))
    }

    it("should create parser for Laptop") {
      val laptopOptions = implicitly[Options[Laptop]]

      val laptop1 = laptopOptions.parse("--cpu.mhz 3000 --cpu.cores 8 --drive.size 2000 --drive.opal true")
      assert(laptop1 === Laptop(Cpu(3000, 8), Drive(2000, true)))

      val laptop2 = laptopOptions.parse("--cpu.mhz 2000 --drive.size 1000")
      assert(laptop2 === Laptop(Cpu(2000, 4), Drive(1000, false)))

      intercept[IllegalArgumentException](laptopOptions.parse("--cpu.mhz 2000"))
    }

    it("should create parser for VM") {
      val vmOptions = implicitly[Options[VM]]

      val vm1 = vmOptions.parse("--host.name someserver --host.ip 192.168.1.1 --tag Name=MyVM")
      assert(vm1 === VM(("Name", "MyVM"), Server("someserver", "192.168.1.1"), 0))
    }

    it("should create parser for Empty") {
      val emptyOptions = implicitly[Options[Empty]]

      val empty1 = emptyOptions.parse(" ")
      assert(empty1 === Empty())
    }

    it("should create parser for NestedEmpty") {
      val nestedEmptyOptions = implicitly[Options[NestedEmpty]]

      val nestedEmpty1 = nestedEmptyOptions.parse(" ")
      assert(nestedEmpty1 === NestedEmpty(Empty()))
    }

    it("should create a parser for Config") {
      val configOptions = implicitly[Options[Config]]

      val config1 = configOptions.parse("--out /path/to/some/file")
      assert(config1 === Config().copy(out = new File("/path/to/some/file")))
    }

    it("should create a parser for OtherConfig") {
      val otherConfigOptions = implicitly[Options[OtherConfig]]

      val otherConfig1 = otherConfigOptions.parse("--path /path/to/some/file")
      assert(otherConfig1 === OtherConfig().copy(path = Some(new File("/path/to/some/file"))))

      val otherConfig2 = otherConfigOptions.parse(" ")
      assert(otherConfig2 === OtherConfig())
    }

    it("should create a parser for NestedDefaults") {
      val nestedDefaultsOptions = implicitly[Options[NestedDefaults]]

      val nestedDefaults1 = nestedDefaultsOptions.parse("--switch false")
      println(nestedDefaults1)
      assert(nestedDefaults1 === NestedDefaults(defaults = Defaults()).copy(switch = false))
    }
  }
}
