# recline

recline is an opinionated derivation framework for the excellent [decline](https://github.com/bkirwi/decline) command line parser.

## Motivation

This library is designed to be used with microservices where you'd like to configure them mostly through a configuration file, environment variables, or command line flags.

It assumes that command line flags should override environment variables, and they should both override the configuration file.

## Modes

It includes two modes, one which will derive an `Opt[T]` where `T` is your configuration case class, this will require most options to be passed in on the command line or via environment variables.

The second mode allows you to derive an `Opt[T => T]` which is intended to be layered on top of a configuration file, or a set of defaults.

