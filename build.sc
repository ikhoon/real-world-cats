import mill._
import mill.scalalib._

object tips extends ScalaModule {
  def scalaVersion = "2.12.4"

  def scalacOptions = Seq(
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-Ypartial-unification"
  )


  def scalacPluginIvyDeps = Agg(
    ivy"org.spire-math::kind-projector:0.9.6"
  )

  def ivyDeps = Agg(
    ivy"org.typelevel::cats-core:1.0.1",
    ivy"org.typelevel::cats-free:1.0.1",
    ivy"com.twitter::util-core:18.3.0",
    ivy"io.catbird::catbird-util:18.3.0",
    ivy"io.monix::monix:3.0.0-RC1",
    ivy"org.scala-lang.modules::scala-java8-compat:0.8.0",
    ivy"org.typelevel::cats-mtl-core:0.2.1"
  )

  object test extends Tests{
    def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.6.0")
    def testFrameworks = Seq("utest.runner.Framework")
  }
}
