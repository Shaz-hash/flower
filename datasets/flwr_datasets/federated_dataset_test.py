# Copyright 2023 Flower Labs GmbH. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ==============================================================================
"""Federated Dataset tests."""
import unittest

import pytest
from parameterized import parameterized, parameterized_class

import datasets
from flwr_datasets.federated_dataset import FederatedDataset


@parameterized_class(
    [
        {"dataset_name": "mnist"},
        {"dataset_name": "cifar10"},
    ]
)
class RealDatasetsFederatedDatasets(unittest.TestCase):
    """Test Real Dataset (MNIST, CIFAR10) in FederatedDatasets."""

    dataset_name = ""

    @parameterized.expand(  # type: ignore
        [
            (
                "10",
                10,
            ),
            (
                "100",
                100,
            ),
        ]
    )
    def test_load_partition_size(self, _: str, train_num_partitions: int) -> None:
        """Test if the partition size is correct based on the number of partitions."""
        dataset_fds = FederatedDataset(
            dataset=self.dataset_name, partitioners={"train": train_num_partitions}
        )
        dataset_partition0 = dataset_fds.load_partition(0, "train")
        dataset = datasets.load_dataset(self.dataset_name)
        self.assertEqual(
            len(dataset_partition0), len(dataset["train"]) // train_num_partitions
        )

    def test_load_full(self) -> None:
        """Test if the load_full works on the correct split name."""
        dataset_fds = FederatedDataset(
            dataset=self.dataset_name, partitioners={"train": 100}
        )
        dataset_fds_test = dataset_fds.load_full("test")
        dataset_test = datasets.load_dataset(self.dataset_name)["test"]
        self.assertEqual(len(dataset_fds_test), len(dataset_test))

    def test_multiple_partitioners(self) -> None:
        """Test if the dataset works when multiple partitioners are specified."""
        num_train_partitions = 100
        num_test_partitions = 100
        dataset_fds = FederatedDataset(
            dataset=self.dataset_name,
            partitioners={"train": num_train_partitions, "test": num_test_partitions},
        )
        dataset_test_partition0 = dataset_fds.load_partition(0, "test")

        dataset = datasets.load_dataset(self.dataset_name)
        self.assertEqual(
            len(dataset_test_partition0), len(dataset["test"]) // num_test_partitions
        )


class IncorrectUsageFederatedDatasets(unittest.TestCase):
    """Test incorrect usages in FederatedDatasets."""

    def test_no_partitioner_for_split(self) -> None:  # pylint: disable=R0201
        """Test using load_partition with missing partitioner."""
        dataset_fds = FederatedDataset(dataset="mnist", partitioners={"train": 100})

        with pytest.raises(ValueError):
            dataset_fds.load_partition(0, "test")

    def test_no_split_in_the_dataset(self) -> None:  # pylint: disable=R0201
        """Test using load_partition with non-existent split name."""
        dataset_fds = FederatedDataset(
            dataset="mnist", partitioners={"non-existent-split": 100}
        )

        with pytest.raises(ValueError):
            dataset_fds.load_partition(0, "non-existent-split")


if __name__ == "__main__":
    unittest.main()