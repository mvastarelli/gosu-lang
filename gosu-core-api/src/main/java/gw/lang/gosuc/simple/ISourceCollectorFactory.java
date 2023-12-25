package gw.lang.gosuc.simple;

import gw.lang.gosuc.cli.CommandLineOptions;

@FunctionalInterface
public interface ISourceCollectorFactory {
  ISourceCollector create(CommandLineOptions options);
}
