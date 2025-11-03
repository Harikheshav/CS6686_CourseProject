// See LICENSE for license details.
package chipyard.fpga.vc707

import org.chipsalliance.cde.config.Config
import chipyard.config._
import gemmini._

// Linux-capable Rocket core + Gemmini for VC707
class VC707GemminiConfig extends Config(
  new gemmini.DefaultGemminiConfig ++           // attach Gemmini accelerator
  new WithVC707Tweaks ++              // reuse existing board clock, DDR, UART, etc.
  new WithFPGAFrequency(25) ++
  new chipyard.config.WithBroadcastManager ++   // single-level coherence
  new chipyard.RocketConfig                     // standard Rocket base
)
