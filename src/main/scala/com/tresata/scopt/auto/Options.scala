package com.tresata.scopt.auto

import scala.reflect.runtime.universe.TypeTag

import scopt.{OParser, Read}

import shapeless.{::, Default, Generic, HList, HNil, LabelledGeneric, Poly1, Witness}
import shapeless.labelled.FieldType
import shapeless.ops.hlist.{Mapper, ToTraversable}
import shapeless.ops.record.Modifier

trait Options[C] extends Serializable {
  def parser: OParser[_, C]

  def parse(args: Array[String]): C

  def parse(s: String): C = parse(s.split("\\s+"))
}

object Options {
  implicit def forProduct[C <: Product, R <: HList, D <: HList](implicit
    gen: LabelledGeneric.Aux[C, R],
    pb: ProductBuilder[R, R],
    de: Default.AsOptions.Aux[C, D],
    tr: ToTraversable.Aux[D, List, Option[Any]],
    em: Empty[C]
  ): Options[C] = new Options[C] {
    def parser: OParser[_, C] = {
      val builder = OParser.builder[C]
      import builder._

      pb.parser(gen, de.apply.toList[Option[Any]]) match {
        case Some(parser) => OParser.sequence(parser, help("help"))
        case None => help("help")
      }
    }

    def parse(args: Array[String]): C = OParser.parse(parser, args, em.empty) match {
      case Some(c) => c
      case None => throw new IllegalArgumentException("invalid arguments")
    }
  }
}

private[auto] object Shared {
  def toSnakeCase(name: String): String = name.flatMap(x => if (x.isUpper) s"-${x.toLower}" else s"${x}" ).dropWhile(_ == '-')
}
import Shared._

trait Empty[C] extends Serializable {
  def empty: C
}

object Empty {
  object unwrapOption extends Poly1 {
    implicit def forProduct[C <: Product](implicit empty: Empty[C]) = at[Option[C]]{ c => c.getOrElse(empty.empty) }

    implicit def forDefault[T] = at[Option[T]]{ s => s.getOrElse(null.asInstanceOf[T]) }
  }

  implicit def forProduct[C <: Product, R <: HList, D <: HList](implicit
    gen: Generic.Aux[C, R],
    de: Default.AsOptions.Aux[C, D],
    map: shapeless.Strict[Mapper.Aux[unwrapOption.type, D, R]] // beats me why i need this strict here
  ): Empty[C] = new Empty[C] {
    def empty: C = {
      implicit def actualMap: Mapper.Aux[unwrapOption.type, D, R] = map.value

      gen.from(de().map(unwrapOption))
    }
  }
}

trait ProductBuilder[R, R1] extends Serializable {
  def parser[C](gen: LabelledGeneric.Aux[C, R1], de: List[Option[Any]]): Option[OParser[_, C]]
}

private[auto] trait LowPriorityProductBuilderImplicits {
  implicit def forHList1[K <: Symbol, V <: Product, T <: HList, R1 <: HList, R2 <: HList, D <: HList](implicit
    w: Witness.Aux[K],
    // start of implicits for nested case class
    hgen: LabelledGeneric.Aux[V, R2],
    hpb: NestedProductBuilder[R2, R2],
    hde: Default.AsOptions.Aux[V, D],
    htr: ToTraversable.Aux[D, List, Option[Any]],
    // end of implicits for nested case class
    mod: Modifier.Aux[R1, K, V, V, R1],
    tpb: ProductBuilder[T, R1]
  ): ProductBuilder[FieldType[K, V] :: T, R1] = new ProductBuilder[FieldType[K, V] :: T, R1] {

    def parser[C](gen: LabelledGeneric.Aux[C, R1], de: List[Option[Any]]): Option[OParser[_, C]] = {
      val name = toSnakeCase(w.value.name)
      val defaults = hde.apply.toList[Option[Any]]

      Some(hpb.parser(gen, mod, name, hgen, defaults).toSeq ++ tpb.parser(gen, de.tail).toSeq)
        .filter(_.nonEmpty)
        .map(s => OParser.sequence(s.head, s.tail: _*))
    }
  }
}

object ProductBuilder extends LowPriorityProductBuilderImplicits {
  implicit def forHList[K <: Symbol, V, T <: HList, R1 <: HList](implicit
    w: Witness.Aux[K],
    r: Read[V],
    tt: TypeTag[V],
    mod: Modifier.Aux[R1, K, V, V, R1],
    tpb: ProductBuilder[T, R1]
  ): ProductBuilder[FieldType[K, V] :: T, R1] = new ProductBuilder[FieldType[K, V] :: T, R1] {

    def parser[C](gen: LabelledGeneric.Aux[C, R1], de: List[Option[Any]]): Option[OParser[V, C]] = {
      val builder = OParser.builder[C]
      import builder._

      val name = toSnakeCase(w.value.name)
      val required = de.head.map{ _ => "optional" }.getOrElse("required")
      val default = de.head.map{ x => s" (default ${x})" }.getOrElse("")
      val o = opt[V](name)
        .text(s"${required} ${tt.tpe}${default}")
        .action{ (v: V, c: C) => gen.from(mod(gen.to(c), _ => v)) }

      Some(OParser.sequence(
        de.head.map{ _ => o.optional()}.getOrElse(o.required()),
        tpb.parser(gen, de.tail).toSeq: _*
      ))
    }
  }

  implicit def forHNil[R1 <: HList]: ProductBuilder[HNil, R1] = new ProductBuilder[HNil, R1] {
    def parser[C](gen: LabelledGeneric.Aux[C, R1], de: List[Option[Any]]): Option[OParser[Unit, C]] = None
  }
}

trait NestedProductBuilder[R, R1] extends Serializable {
  def parser[C0, R0 <: HList, K0, C](gen0: LabelledGeneric.Aux[C0, R0], mod0: Modifier.Aux[R0, K0, C, C, R0], name0: String,
      gen: LabelledGeneric.Aux[C, R1], de: List[Option[Any]]): Option[OParser[_, C0]]
}

object NestedProductBuilder {
  implicit def forHList[K <: Symbol, V, T <: HList, R1 <: HList](implicit
    w: Witness.Aux[K],
    r: Read[V],
    tt: TypeTag[V],
    mod: Modifier.Aux[R1, K, V, V, R1],
    tpb: NestedProductBuilder[T, R1]
  ): NestedProductBuilder[FieldType[K, V] :: T, R1] = new NestedProductBuilder[FieldType[K, V] :: T, R1] {
    
    def parser[C0, R0 <: HList, K0, C](gen0: LabelledGeneric.Aux[C0, R0], mod0: Modifier.Aux[R0, K0, C, C, R0], name0: String,
      gen: LabelledGeneric.Aux[C, R1], de: List[Option[Any]]): Option[OParser[V, C0]] = {
      val builder = OParser.builder[C0]
      import builder._

      val name = s"${name0}.${toSnakeCase(w.value.name)}"
      val required = de.head.map{ _ => "optional" }.getOrElse("required")
      val default = de.head.map{ x => s" (default ${x})" }.getOrElse("")
      val o = opt[V](name)
        .text(s"${required} ${tt.tpe}${default}")
        .action{ (v: V, c0: C0) =>
          gen0.from(mod0(gen0.to(c0), { c =>
            gen.from(mod(gen.to(c), _ => v))
          }))
        }
      
      Some(OParser.sequence(
        de.head.map{ _ => o.optional()}.getOrElse(o.required()),
        tpb.parser(gen0, mod0, name0, gen, de.tail).toSeq: _*
      ))
    }
  }

  implicit def forHNil[R1 <: HList]: NestedProductBuilder[HNil, R1] = new NestedProductBuilder[HNil, R1] {
    def parser[C0, R0 <: HList, K0, C](gen0: LabelledGeneric.Aux[C0, R0], mod0: Modifier.Aux[R0, K0, C, C, R0], name0: String,
      gen: LabelledGeneric.Aux[C, R1], de: List[Option[Any]]): Option[OParser[_, C0]] = None
  }
}
