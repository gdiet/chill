/*
Copyright 2012 Twitter, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.twitter.chill

import org.specs._

import com.twitter.chill.config._

import com.esotericsoftware.shaded.org.objenesis.strategy.InstantiatorStrategy;
import com.esotericsoftware.shaded.org.objenesis.strategy.StdInstantiatorStrategy;


class ReflectingInstantiatorTest extends Specification {
  "A ReflectingInstantiator" should {
    "set keys into a config as expected" in {

      val ri = ReflectingInstantiatorBuilder(instantiatorStrategyClass = classOf[InstantiatorStrategy],
        classes = Iterable(new ClassRegistrar(classOf[List[_]])),
        serializers = Iterable(new ReflectingRegistrar(classOf[List[_]], classOf[com.esotericsoftware.kryo.serializers.JavaSerializer])),
        defaults = Iterable(new ReflectingDefaultRegistrar(classOf[List[_]], classOf[com.esotericsoftware.kryo.serializers.JavaSerializer])),
        skipMissing = true,
        registrationRequired = true).build

      val conf = ScalaMapConfig.empty
      ri.set(conf)

      conf.toMap(ReflectingInstantiator.KRYO_CLASS) must be_==(classOf[Kryo].getName)
      conf.toMap(ReflectingInstantiator.INSTANTIATOR_STRATEGY_CLASS) must be_==(classOf[InstantiatorStrategy].getName)
      conf.toMap(ReflectingInstantiator.REGISTRATION_REQUIRED) must be_==("true")
      conf.toMap(ReflectingInstantiator.SKIP_MISSING) must be_==("true")
      conf.toMap(ReflectingInstantiator.REGISTRATIONS).split(":").toSet must be_== (
        Set("scala.collection.immutable.List",
          "scala.collection.immutable.List,com.esotericsoftware.kryo.serializers.JavaSerializer")
      )
      conf.toMap(ReflectingInstantiator.DEFAULT_REGISTRATIONS).split(":").toSet must be_== (
        Set("scala.collection.immutable.List,com.esotericsoftware.kryo.serializers.JavaSerializer")
      )
    }
    "roundtrip through a config" in {
      val ri = ReflectingInstantiatorBuilder(instantiatorStrategyClass = classOf[StdInstantiatorStrategy],
        classes = Iterable(new ClassRegistrar(classOf[List[_]])),
        serializers = Iterable(new ReflectingRegistrar(classOf[List[_]], classOf[com.esotericsoftware.kryo.serializers.JavaSerializer])),
        defaults = Iterable(new ReflectingDefaultRegistrar(classOf[List[_]], classOf[com.esotericsoftware.kryo.serializers.JavaSerializer])),
        skipMissing = true,
        registrationRequired = true).build

      val conf = ScalaMapConfig.empty
      ri.set(conf)
      val ri2 = new ReflectingInstantiator(conf)
      ri.equals(ri2) must be_==(true)
      ri2.equals(ri) must be_==(true)
    }

  "be serialized when added onto a ConfiguredInstantiator" in {
      val conf = new JavaMapConfig
      ConfiguredInstantiator.setReflect(conf, classOf[ScalaKryoInstantiator])
       val instantiator = new ConfiguredInstantiator(conf).getDelegate()
         .withRegistrar {
           new ReflectingDefaultRegistrar(classOf[List[_]], classOf[com.esotericsoftware.kryo.serializers.JavaSerializer])
       }
      try {
        ConfiguredInstantiator.setSerialized(
         conf,
         classOf[ScalaKryoInstantiator],
         instantiator
        )
       }catch{
         case e => fail("Got exception serializing the instantiator\n" + e.printStackTrace)
       }
    }
  }
}
