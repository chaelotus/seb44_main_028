import { useQuery } from 'react-query';
import axios from 'axios';
import CheckBox from './CheckBox';
import { CategoryListProps } from '../../type';
import { ICategory } from '../../model/ICategory';
import Loading from '../Loading';
import ErrorPage from '../ErrorPage';
import { QUERY_KEY } from '../../utils/queryKet';

const CheckBoxList = ({
  selectedtCategory,
  setSelectedCategory,
}: CategoryListProps) => {
  const fetchCategoryData = async () => {
    const response = await axios.get(
      `${process.env.REACT_APP_API_URL}/api/categories`,
    );
    return response.data;
  };
  const {
    data: category,
    isLoading,
    isError,
  } = useQuery<ICategory[]>(QUERY_KEY.CATEGORY, fetchCategoryData);
  if (isLoading) {
    return <Loading />;
  }
  if (isError) {
    return <ErrorPage />;
  }
  console.log('체크된 값', selectedtCategory);
  return (
    <div>
      {category?.map((checkbox, index) => {
        return (
          <CheckBox
            key={index}
            categoryTitle={checkbox.title}
            categoryId={checkbox.categoryId}
            selectedtCategory={selectedtCategory}
            setSelectedCategory={setSelectedCategory}
          />
        );
      })}
    </div>
  );
};

export default CheckBoxList;
