KVMDirect
=========

Java tool that connects to remote KVM server (iDRAC, ILO, …) and launches virtual KVM console.

Currently supported:
 * HP
   * ILO1 (Applet mode)
   * ILO2 (Applet mode)
   * ILO3 (Applet mode)
 * Supermicro
   * X7 (Applet mode, keyboard may be not working, tested on single old server of unknown reliability)
   * X8 (JNLP mode)
   * Aten (X9 and newer, JNLP mode)
 * DELL (JNLP mode)
   * IDRAC8
   * IDRAC9
 * IBM (JNLP mode)
   * M3
   * M4

Warning hacky software!
-----------------------

This software is my first Java project (and may as well be also last). Thus there is lot of dirty
code. Clean code is not my priority with this project, and I did not try to learn Java standards.
Just hack it together, so it works. On the other hand I hate not reusable code, so big chunk is
written in what will be reusable form, if coding standards were followed.

Applet mode
-----------

Browser environment is emulated only to what is needed to launch applets of supported machines.

JNLP mode
---------

Behaviour of javaws binary is emulated, without all security checks.
Java interpreter is then launched on downloaded jars.

Settings
--------

Settings should be provided in `kvm.ini`. See `kvm.ini.exmaple` for information what can be set.

Security
--------

All security measures regarding SSL are turned off if you use setting in `kvm.ini.example`.
This is often neccessary to run remote console. Also few or no checks are done before executing
downloaded jars. Thus be careful and use this only in enviromnet, where attacker cannot tamper
with remote server or network between you and remote server.

Building
--------
Use gradle task `fatJar` to build single jar file, that can be run with `java -jar …`

Running
-------
Just specify hostname (if it does not contain `=`) as single argument.
You can add other configuration arguments as `key=value` on command line,
supported are all values that are in kvm.ini.

Configuration format
--------------------
Ini style sections are only syntactic sugar. They are just prepended before
configruation property names. `kvm.ini` uses in fact java properties file
format, enhanced with ini style sections.

KVMDirect configuratio keys consist of two parts:
 * selector (starts with one of `arguments`, `host`, `module`, `default` and `results`)
 * actual property name (starts with one of `kvm`, `security`, `system`, `custom`)

Thus `host.my-http-server.kitchen.example.com.kvm.user` is valid name with
selector `host.my-http-server.kitchen.example.com` and property `kvm.user`.

Selector define if the configuration is applied (`arguments.main`,
`host.name-of-host.provided.on.commmand-line`,
`module.ilo.ILO2`, `default`, `results.low`, `results.high` )
and also priority when multiple selectors are used
( `default` < `module` < `results.low` < `host` < `arguments.main` < `results.high` )

Queries (dynamic configuration)
-------------------------------
You can query via http external source of configuration, and parse result using
JSoup (HTML/XML parser) and regular expressions. See `kvm.ini.example` for examples.

Queries are launched in alfabetical order and result of query is stored with
selector `results.low` or `results.high` (if name is prefixed by exclamation mark).
