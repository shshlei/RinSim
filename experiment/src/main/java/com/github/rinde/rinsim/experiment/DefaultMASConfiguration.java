/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.experiment;

import java.io.Serializable;

import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * Implementation of {@link MASConfiguration} which implements most methods
 * using default values.
 * @author Rinde van Lon
 */
public abstract class DefaultMASConfiguration implements MASConfiguration,
  Serializable {
  private static final long serialVersionUID = 4815504615843930209L;

  @Override
  public ImmutableList<? extends ModelBuilder<?, ?>> getModels() {
    return ImmutableList.of();
  }

  @Override
  public Optional<? extends TimedEventHandler<AddDepotEvent>> getDepotCreator() {
    return Optional.absent();
  }

  @Override
  public Optional<? extends TimedEventHandler<AddParcelEvent>> getParcelCreator() {
    return Optional.absent();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
